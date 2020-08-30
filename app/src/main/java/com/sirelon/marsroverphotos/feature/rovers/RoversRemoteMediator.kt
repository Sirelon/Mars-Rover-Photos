package com.sirelon.marsroverphotos.feature.rovers

import android.os.Build
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.network.RestApi
import com.sirelon.marsroverphotos.storage.RoverDao
import kotlinx.coroutines.flow.asFlow
import java.util.stream.Collectors
import kotlin.streams.toList

/**
 * Created on 30.08.2020 21:47 for Mars-Rover-Photos.
 */
//@ExperimentalPagingApi
//class RoversRemoteMediator(private val api: RestApi, private val roverDao: RoverDao) : RemoteMediator<Int, Rover>() {
//
//    override suspend fun load(loadType: LoadType, state: PagingState<Int, Rover>): MediatorResult {
//        val roversName = listOf("curiosity", "opportunity", "spirit")
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            val list = roversName
//                .parallelStream()
//                .map { api.getRoverInfo(it).blockingFirst() }
//                .collect(Collectors.toList())
//
//            roverDao.insertRoversList(list)
//        } else {
//            val list = roversName
//                .map { api.getRoverInfo(it).blockingFirst() }
//            roverDao.insertRoversList(list)
//        }
//    }
//
//}