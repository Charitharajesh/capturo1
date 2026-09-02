package com.capturo.app.domain.usecase.creator

import com.capturo.app.data.repository.CreatorRepository
import javax.inject.Inject

class GetNearbyCreatorsUseCase @Inject constructor(
    private val repository: CreatorRepository
) {
    operator fun invoke(lat: Double, lon: Double, radius: Double) =
        repository.getNearbyCreators(lat, lon, radius)
}
