#!/usr/bin/env ruby
# Uploads the generated store screenshots (store/assets/) to App Store Connect and Google Play.
#
#   bundle exec ruby store/tools/upload-screenshots.rb status             # read-only: versions and current sets
#   bundle exec ruby store/tools/upload-screenshots.rb app-store          # dry run: prints what would change
#   bundle exec ruby store/tools/upload-screenshots.rb app-store --apply  # replaces the screenshot sets
#   bundle exec ruby store/tools/upload-screenshots.rb play --apply
#
#   bundle exec ruby store/tools/upload-screenshots.rb app-store --create-version=5.6.0 --apply
#
# App Store: writes into the iOS version that is still editable (PREPARE_FOR_SUBMISSION and the
# other pre-review states). A version in review or on sale is locked: the script stops there, or
# with --create-version creates the next draft first. A draft ships nothing; the next release's
# deliver run renames it to the real version number (Spaceship's ensure_version!).
# Google Play: replaces phone, 7" and 10" tablet screenshots of the listing in one edit and
# commits it, which publishes the listing change at once.
# Run with LANG=en_US.UTF-8 (fastlane requires it). Keys: fastlane/ (gitignored, see docs/RELEASE.md).

require "spaceship"
require "supply"

ROOT = File.expand_path("../..", __dir__)
ASSETS = File.join(ROOT, "store/assets")
APPLY = ARGV.include?("--apply")
NEW_VERSION = ARGV.find { |a| a.start_with?("--create-version=") }&.split("=", 2)&.last
BUNDLE_ID = "com.sirelon.marsroverphotos"

# The listing is English only on both stores.
APP_STORE_LOCALE = "en-US"
PLAY_LANGUAGE = "en-US"

# Display type -> generated folder. The API has no 6.9" or 13" type: APP_IPHONE_67 takes the
# 1320x2868 images, APP_IPAD_PRO_3GEN_129 the 2752x2064 ones. APP_IPHONE_65 keeps its own
# 1284x2778 set so 6.5" devices do not fall back to an older one.
APP_STORE_SLOTS = {
  "APP_IPHONE_67" => "iphone-6.9",
  "APP_IPHONE_65" => "iphone-6.5",
  "APP_IPAD_PRO_3GEN_129" => "ipad-13",
}.freeze
PLAY_SLOTS = {
  "phoneScreenshots" => "phone",
  "sevenInchScreenshots" => "tablet",
  "tenInchScreenshots" => "tablet",
}.freeze

def images(dir)
  Dir.glob(File.join(dir, "*.jpg")).sort
end

def asc_app
  Spaceship::ConnectAPI.token = Spaceship::ConnectAPI::Token.create(
    key_id: "G5TTXS7GV3", issuer_id: "8873b38c-775e-4a3e-baf5-4e2bada765a8",
    filepath: File.join(ROOT, "fastlane/AuthKey_G5TTXS7GV3.p8"),
  )
  Spaceship::ConnectAPI::App.find(BUNDLE_ID)
end

def play_client
  Supply.config = FastlaneCore::Configuration.create(Supply::Options.available_options, {
    package_name: BUNDLE_ID, json_key: File.join(ROOT, "fastlane/google-play-key.json"),
  })
  client = Supply::Client.make_from_config
  client.begin_edit(package_name: BUNDLE_ID)
  client
end

def status
  app = asc_app
  versions = app.get_app_store_versions(filter: { platform: "IOS" })
  versions.first(3).each { |v| puts "App Store #{v.version_string}: #{v.app_store_state}" }
  shown = app.get_edit_app_store_version(platform: Spaceship::ConnectAPI::Platform::IOS) || versions.first
  puts "Screenshot sets of #{shown.version_string}:"
  shown.get_app_store_version_localizations.each do |loc|
    loc.get_app_screenshot_sets.each do |set|
      puts "  #{loc.locale} #{set.screenshot_display_type}: #{set.app_screenshots&.size || 0}"
    end
  end
  client = play_client
  client.listings.each do |listing|
    (PLAY_SLOTS.keys + ["featureGraphic"]).each do |type|
      puts "Play #{listing.language} #{type}: #{client.fetch_images(image_type: type, language: listing.language).size}"
    end
  end
  client.abort_current_edit
end

def app_store
  app = asc_app
  ios = Spaceship::ConnectAPI::Platform::IOS
  version = app.get_edit_app_store_version(platform: ios)
  if version.nil?
    latest = app.get_app_store_versions(filter: { platform: "IOS" }).first
    puts "Latest iOS version: #{latest.version_string} (#{latest.app_store_state}): screenshots locked"
    unless NEW_VERSION
      abort "No editable version. Pass --create-version=X.Y.Z to create the next draft, " \
            "or run again once the store-release skill has created it."
    end
    puts "Create draft #{NEW_VERSION} (PREPARE_FOR_SUBMISSION)"
    return puts("Dry run. Add --apply to create it and upload.") unless APPLY
    app.ensure_version!(NEW_VERSION, platform: ios)
    version = app.get_edit_app_store_version(platform: ios)
  end
  puts "Editable iOS version: #{version.version_string} (#{version.app_store_state})"
  loc = version.get_app_store_version_localizations.find { |l| l.locale == APP_STORE_LOCALE }
  abort "No #{APP_STORE_LOCALE} localization on #{version.version_string}" unless loc
  sets = loc.get_app_screenshot_sets
  APP_STORE_SLOTS.each do |type, folder|
    files = images(File.join(ASSETS, "app-store", folder))
    next puts("#{type}: no images in app-store/#{folder}, left alone") if files.empty?
    set = sets.find { |s| s.screenshot_display_type == type }
    puts "#{APP_STORE_LOCALE} #{type}: replace #{set&.app_screenshots&.size || 0} with #{files.size} (#{folder})"
    next unless APPLY
    set ||= loc.create_app_screenshot_set(attributes: { screenshotDisplayType: type })
    (set.app_screenshots || []).each(&:delete!)
    files.each { |f| set.upload_screenshot(path: f, wait_for_processing: false) }
  end
  puts(APPLY ? "Uploaded. Check processing in App Store Connect." : "Dry run. Add --apply to upload.")
end

def play
  client = play_client
  begin
    PLAY_SLOTS.each do |type, folder|
      files = images(File.join(ASSETS, "play-store", folder))
      next puts("#{type}: no images in play-store/#{folder}, left alone") if files.empty?
      old = client.fetch_images(image_type: type, language: PLAY_LANGUAGE).size
      puts "#{PLAY_LANGUAGE} #{type}: replace #{old} with #{files.size} (#{folder})"
      next unless APPLY
      client.clear_screenshots(image_type: type, language: PLAY_LANGUAGE)
      files.each { |f| client.upload_image(image_path: f, image_type: type, language: PLAY_LANGUAGE) }
    end
    if APPLY
      client.commit_current_edit!
      puts "Committed: the listing change is live on Google Play."
    else
      client.abort_current_edit
      puts "Dry run. Add --apply to upload."
    end
  rescue StandardError
    client.abort_current_edit rescue nil
    raise
  end
end

case ARGV.first
when "status" then status
when "app-store" then app_store
when "play" then play
else abort "usage: bundle exec ruby store/tools/upload-screenshots.rb status|app-store|play [--apply] [--create-version=X.Y.Z]"
end
