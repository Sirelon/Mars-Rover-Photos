# Adding more Mars surface missions to your photo app

**All URLs below were fetched and confirmed on 2026-08-08.** Dead missions included — that's the point.

You currently show 4 rovers (Curiosity, Perseverance, Spirit, Opportunity). There are **7 more Mars surface missions with retrievable, displayable imagery**, plus a pile of cameras your current APIs silently omit. Roster goes 4 → 11.

---

## The one-line win: Ingenuity

Your existing `mars.nasa.gov/rss/api` feed has an undocumented category. **VERIFIED:**

```
https://mars.nasa.gov/rss/api/?feed=raw_images&category=ingenuity&feedtype=json&num=25&page=0&order=sol+desc
```

- **`total_results: 14553`** — the entire Ingenuity flight campaign
- Two cameras: `HELI_NAV` (640×480 mono, filename prefix `HNM_`) and `HELI_RTE` (**4208×3120 colour**, prefix `HSF_`, ~608 images)
- Last sol **1069 = 2024-02-22**, the final flight
- Identical record shape to your Mars 2020 ingest — same `image_files` object with `full_res` (PNG) + `large`/`medium`/`small` (JPEG at 1200/800/320)
- `category=mars2020,ingenuity` accepts a comma list, so you can pull both in one call

This is a config change, not an integration. A helicopter is also a genuinely different UI story than a rover — flight numbers, altitude, downward-looking nav frames.

**Negative result, so you don't waste an afternoon:** `category=insight` returns a real 404 (`{"message": "This page was not found..."}`). So do `phoenix`, `mer`, `nsyt`. Ingenuity is the *only* extra category on that feed.

---

## The actual unlock: `planetarydata.jpl.nasa.gov`

This is the finding that matters. It's a **plain Apache autoindex over HTTPS, no auth, no key, sol-partitioned — and the JPEG browse images are already generated.** You never touch a VICAR file.

```
https://planetarydata.jpl.nasa.gov/img/data/
```

I verified open directory listings and real browse images in every tree below.

### Missions you don't have yet

| Mission | Years | Browse path (verified) | Format |
|---|---|---|---|
| **Viking Lander 1** | 1976–1982 | `/img/data/vl1_vl2-m-lcs-2-edr-v1.0/vl_0001/extras/browse/{a-j}{0-2}xx/` | **`.jpeg`** full-size |
| **Viking Lander 2** | 1976–1980 | `.../vl_0002/extras/browse/...` | `.jpeg` |
| **Mars Pathfinder (IMP)** | 1997 | `pds.nasa.gov/data/mpfl-m-imp-2-edr-v1.0/mpim_000{1,2,3}/browse/{target}/seq####/c#######/` | `.gif` (+ tiny `.jpg` thumb) |
| **Sojourner rover** | 1997 | `pds.nasa.gov/data/mpfr-m-rvrcam-2-edr-v1.0/mprv_0001/browse/rvr_edr/{rvr_left,rvrright,rvr_rear,rvr_clr}/` | `.gif` |
| **Phoenix** | 2008 | `/img/data/phoenix/phx{ssi,rac,om}_0xxx/extras/browse/sol{NNN}/` | **`.img.jpeg`** |
| **InSight** | 2018–2022 | `/img/data/nsyt/insight_cameras/browse/sol/{NNNN}/mipl/edr/{idc,icc}/` | **`.PNG`** ~1.5 MB |

Verified samples I actually pulled listings for:

```
# Viking Lander 1, camera 2, image i202, broadband filter 4
https://planetarydata.jpl.nasa.gov/img/data/vl1_vl2-m-lcs-2-edr-v1.0/vl_0001/extras/browse/i2xx/12i202b4.jpeg   (145K)

# Sojourner left camera
https://pds.nasa.gov/data/mpfr-m-rvrcam-2-edr-v1.0/mprv_0001/browse/rvr_edr/rvr_left/r0128638.gif   (45K)

# Phoenix SSI, sol 112
https://planetarydata.jpl.nasa.gov/img/data/phoenix/phxssi_0xxx/extras/browse/sol112/ss112edn906146774_1cff3lcm1.img.jpeg

# InSight IDC, sol 100
https://planetarydata.jpl.nasa.gov/img/data/nsyt/insight_cameras/browse/sol/0100/mipl/edr/idc/D010L0100_605414345EDR_F0103_0100M2.PNG   (1.5M)
```

**Mission facts for your UI:** Viking = ~6,600 images across both landers, first pictures ever taken from the Martian surface. Pathfinder IMP = **16,661 images** in 83 sols. Sojourner = ~550, the first wheels on Mars. Phoenix = sols 0–152, arctic dig site (its MARDI descent camera was **never operated** — don't build that feature). InSight = sols 0–1470, only two cameras but it's the mission that heard marsquakes.

### Cameras you're missing on rovers you already have

Your current API's camera list is genuinely incomplete. Same host fixes it:

```
/img/data/mer/opportunity/   → mer1{po,no,mo,ho,do,om,mw}_0xxx
/img/data/mer/spirit/        → mer2*
/img/data/msl/               → MSLNAV_0XXX, MSLHAZ_0XXX, MSLMST_*, MSLMHL_*, MSLMRD_*
/img/data/mars2020/          → mars2020_{navcam,hazcam,mastcamz,cachecam,edlcam,helicam}_ops_*
```

Verified listings:

```
https://planetarydata.jpl.nasa.gov/img/data/mer/opportunity/mer1po_0xxx/browse/sol3712/edr/
  → 1p457719328effcee1p2514l2m1.img.jpg   (74K)

https://planetarydata.jpl.nasa.gov/img/data/msl/MSLNAV_0XXX/EXTRAS/BROWSE/SOL03078/
  → NLB_670756753EDR_F0871444NCAM00207M1.JPG   (12K, F=full)
  → NLB_670756753EDR_T0871444NCAM00207M1.JPG   (1.4K, T=thumb)
```

That gets you **MER Microscopic Imager** (`mer{1,2}mo_0xxx` — absent from the NASA API entirely), MER Descam, SuperCam RMI, CacheCam, and the EDL cameras. Note the case difference: MER/M2020 use lowercase `browse/solNNNN/edr/`, MSL uses uppercase `EXTRAS/BROWSE/SOLNNNNN/`.

Also worth knowing: **Opportunity returned "over 342,000 raw images"** per JPL — the ~217k figure floating around is a narrower count. Spirit: ~124,000–128,224.

---

## Build the catalogue from index files, not by scraping HTML

Every PDS3 volume ships a machine-readable cumulative index. Don't crawl Apache listings.

```
{volume}/index/index.tab       fixed-width ASCII, one row per image
{volume}/index/index.lbl       PDS3 label: ROWS, ROW_BYTES, COLUMN offsets
{volume}/index/cumindex.tab    all volumes in the dataset
```

Sizes for scale: Viking `index.tab` 1.3 MB, Pathfinder `edrindex.tab` 12 MB, Phoenix SSI `index.tab` 17 MB.

InSight is PDS4, so it's an inventory CSV instead:
```
/img/data/nsyt/insight_cameras/data/collection_data_inventory_cumulative.tab
```

The browse filename is a pure string transform of the product ID in the index — same stem, different extension/dir. So: parse index → generate browse URL → done. No HTML parsing anywhere in the pipeline.

Rough parse sketch:

```python
# index.lbl gives you START_BYTE/BYTES per COLUMN; index.tab is fixed-width
import re, requests

lbl = requests.get(f"{VOL}/index/index.lbl").text
cols = {}   # name -> (start-1, length)
for blk in re.findall(r"OBJECT\s*=\s*COLUMN(.*?)END_OBJECT\s*=\s*COLUMN", lbl, re.S):
    name  = re.search(r"NAME\s*=\s*(\S+)", blk).group(1)
    start = int(re.search(r"START_BYTE\s*=\s*(\d+)", blk).group(1))
    ln    = int(re.search(r"BYTES\s*=\s*(\d+)", blk).group(1))
    cols[name] = (start - 1, ln)

for line in requests.get(f"{VOL}/index/index.tab").text.splitlines():
    rec = {k: line[s:s+l].strip().strip('"') for k, (s, l) in cols.items()}
    yield rec        # PRODUCT_ID, IMAGE_TIME, INSTRUMENT_NAME, FILTER_NAME, ...
```

`.tab` and `.lbl` are served as `application/octet-stream` — fetch and parse server-side, don't try to stream them into a browser.

---

## Gotchas that will bite you

1. **`.img.jpeg` / `.img.jpg` extensions.** Phoenix and MER browse files have a double extension, so the server sends `application/octet-stream`, not `image/jpeg`. Browsers usually still render from magic bytes, but **re-serve with a corrected Content-Type** rather than betting on it.
2. **No CORS headers, no CDN.** These are bare Apache trees. Fine for server-side fetch, unreliable for direct browser `fetch()`. And don't hotlink in production — mirror to your own storage. It's cheap: browse JPEGs are 20–60 KB, so Viking + Pathfinder + Sojourner + Phoenix is low single-digit GB. InSight PNGs at 1.5 MB each are the exception — transcode those.
3. **Two hosts 403 automated clients**: `pds-imaging.jpl.nasa.gov` and `pdsimage2.wr.usgs.gov`. Use `planetarydata.jpl.nasa.gov` (Viking, Phoenix, InSight, MER, MSL, M2020) and `pds.nasa.gov/data` (Pathfinder, Sojourner). Both work; both were verified today.
4. **Don't list huge parent directories** — `phxssi_0xxx/data/` and `insight_cameras/browse/sol/` time out. Go straight to a sol-level path.
5. **Viking filenames are weird.** Raw files use the *filter* as the extension (`12i201.red`, `12i202.bb4`, `12i205.sun`), and the browse name abbreviates it to two chars (`12i202b4.jpeg`). Also `extras/thumbnail/` uses `.jpeg_small` — skip that dir.
6. **`api.nasa.gov/mars-photos` looks unmaintained.** Its upstream repo was archived 2025-10-08, and [nasa/api-docs#220](https://github.com/nasa/api-docs/issues/220) — Spirit and Opportunity returning nothing — is open and unanswered. Worth a health check in your app.
7. **PDS Analyst's Notebook is not an image source.** But `https://an.rsl.wustl.edu/phx2008/solbrowser/product.aspx?prod={PRODUCT_ID}` renders server-side for **Phoenix**, so you get free "view full provenance" deep links using the same product IDs from `index.tab`. MER and InSight are JS SPAs with opaque internal row IDs — not derivable, don't try.

---

## Zhurong: read this before you plan it

I'd love to tell you to add it. The honest answer is don't, at least not the rover imagery.

- **CLPDS** (`clpds.bao.ac.cn`) is a JavaScript SPA with cookie-session login. No anonymous file tree, no API — the system paper says IPDA protocols "could be adopted," i.e. they aren't. Products are PDS3/PDS4 binary, so you'd convert too.
- **Nobody has mirrored it.** I searched Zenodo, figshare, Hugging Face, Mendeley, GitHub, institutional repos. What exists is *derived* products — e.g. [Zenodo 7620416](https://zenodo.org/record/7620416), 3D rock models from 178 NaTeCam stereo pairs, CC-BY-4.0, but DEMs and point clouds, no camera images.
- **The licensing is the real blocker.** PRC copyright law has no government-works public-domain exemption like 17 U.S.C. §105, so CNSA/CAS imagery is presumptively **copyrighted**. The Planetary Society's own wording: China appears to allow reuse *"for noncommercial purposes with attribution."* That's not a commercial grant. Treat Wikimedia Commons Zhurong files as individually suspect — some are NASA MRO shots (genuinely PD), some are third-party CC, some may be mis-tagged.

**If you want a Tianwen-1 presence without the exposure:** the MoRIC global colour map is published as HiPS PNG tiles under **ODbL-1.0** at `https://alasky.cds.unistra.fr/Planets/CDS_P_Mars_Tianwen1-MoRIC` — drop-in for Aladin Lite or Leaflet, credit NAOC/GRAS. Orbiter, not rover, but it's real Chinese Mars data you can legally ship.

**Mars 3 (USSR, 1971):** 70 scan lines, ~20 seconds, described as featureless grey. There is nothing to display. Make it a text card in a mission timeline, not an image endpoint. NSSDCA — the usual source — is also **offline for maintenance** right now.

---

## Suggested order

1. **Ingenuity** — hours. One param. 14,553 images, four pre-made sizes, zero new infrastructure.
2. **InSight** — half a day. Parse one inventory CSV, mirror PNGs (transcode to JPEG). Two cameras, clean sol structure.
3. **Phoenix** — a day. Three volumes, `index.tab` → `extras/browse/solNNN/`. Fix Content-Type on serve.
4. **Viking 1 & 2** — a day. Highest story value in the whole list: the first photographs ever taken from the surface of another planet.
5. **Pathfinder + Sojourner** — a day. Two missions from one archive, and Sojourner is the first Mars rover.
6. **Backfill missing cameras** on MER/MSL/M2020 from the same tree — MER Microscopic Imager is the standout.

Licensing across all of items 1–6: **NASA, public domain.** No attribution legally required, though crediting NASA/JPL-Caltech is customary and good practice. Don't imply NASA endorsement and don't use NASA logos.

---

## Sources

- [Ingenuity feed](https://mars.nasa.gov/rss/api/?feed=raw_images&category=ingenuity&feedtype=json&num=2) · [MSL raw images](https://mars.nasa.gov/msl/multimedia/raw-images/)
- [PDS data root](https://planetarydata.jpl.nasa.gov/img/data/) · [Viking vl_0001](https://planetarydata.jpl.nasa.gov/img/data/vl1_vl2-m-lcs-2-edr-v1.0/vl_0001/) · [Phoenix volumes](https://planetarydata.jpl.nasa.gov/img/data/phoenix/) · [InSight cameras bundle](https://planetarydata.jpl.nasa.gov/img/data/nsyt/insight_cameras/) · [MER Opportunity browse sol3712](https://planetarydata.jpl.nasa.gov/img/data/mer/opportunity/mer1po_0xxx/browse/sol3712/edr/) · [MSL Navcam browse SOL03078](https://planetarydata.jpl.nasa.gov/img/data/msl/MSLNAV_0XXX/EXTRAS/BROWSE/SOL03078/) · [Mars 2020 bundles](https://planetarydata.jpl.nasa.gov/img/data/mars2020/)
- [Pathfinder IMP mpim_0001](https://pds.nasa.gov/data/mpfl-m-imp-2-edr-v1.0/mpim_0001/) · [Sojourner mprv_0001](https://pds.nasa.gov/data/mpfr-m-rvrcam-2-edr-v1.0/mprv_0001/) · [PDS Geosciences Viking Lander images](https://pds-geosciences.wustl.edu/missions/vlander/images.html) · [PDS Geosciences MER datasets](https://pds-geosciences.wustl.edu/missions/mer/geo_mer_datasets.htm)
- [Phoenix MARDI never operated (MSSS)](https://www.msss.com/all_projects/phoenix-mardi.php) · [Opportunity image count (JPL)](https://www.jpl.nasa.gov/news/six-things-to-know-about-nasas-opportunity-mars-rover/) · [Spirit end of mission](https://www.sciencedaily.com/releases/2011/05/110525122156.htm)
- [nasa/api-docs #220 — Spirit/Opportunity broken](https://github.com/nasa/api-docs/issues/220) · [mars-photo-api camera matrix](https://github.com/corincerami/mars-photo-api/blob/master/README.md)
- [Zenodo 7620416 — Zhurong 3D rocks, CC-BY-4.0](https://zenodo.org/record/7620416) · [Tianwen-1 MoRIC HiPS, ODbL](https://alasky.cds.unistra.fr/MocServer/query?ID=CDS/P/Mars/Tianwen1-MoRIC&get=record&fmt=html) · [Planetary Society on Zhurong image reuse](https://www.planetary.org/space-images/chinas-zhurong-rover-on-mars) · [CLPDS system paper](https://link.springer.com/article/10.1007/s11214-021-00862-3)
