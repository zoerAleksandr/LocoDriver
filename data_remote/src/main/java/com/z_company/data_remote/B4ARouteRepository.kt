package com.z_company.data_remote

import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.coroutines.suspendSave
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.repositories.RemoteRouteRepository
import kotlinx.coroutines.flow.Flow


private const val routeNameClassRemote = "Route"
private const val basicDataNameClassRemote = "BasicData"

class B4ARouteRepository : RemoteRouteRepository {
    override fun saveRoute(route: Route): Flow<ResultState<Unit>> {
        val routeObject = ParseObject(routeNameClassRemote)

//        val basicDataObject = ParseObject(basicDataNameClassRemote)
//
//        val relationBasicData: ParseRelation<ParseObject> =
//            routeObject.getRelation("basicData")
//        relationBasicData.add(basicDataObject)

        return ResultState.flowRequest {
            routeObject.suspendSave()
        }
    }

    override fun saveBasicData(basicData: BasicData): Flow<ResultState<Unit>> {
        val basicDataObject = ParseObject(basicDataNameClassRemote)
        basicData.number?.let { number ->
            basicDataObject.put("number", number)
        }
        basicData.timeStartWork?.let { time ->
            basicDataObject.put("timeStartWork", time)
        }
        basicData.timeEndWork?.let { time ->
            basicDataObject.put("timeEndWork", time)
        }
        basicDataObject.put("restPointOfTurnover", basicData.restPointOfTurnover)
        basicData.notes?.let { notes ->
            basicDataObject.put("notes", notes)
        }
        return ResultState.flowRequest {
            basicDataObject.suspendSave()
        }
    }
}