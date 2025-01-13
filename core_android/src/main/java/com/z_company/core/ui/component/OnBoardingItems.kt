package com.z_company.core.ui.component

import com.z_company.core.R

class OnBoardingItems(
    val image: Int,
    val title: Int,
    val desc: Int
) {
    companion object{
        fun getDataFirstPresentation(): List<OnBoardingItems>{
            return listOf(
                OnBoardingItems(R.drawable.onboarding_first_screen, R.string.onBoardingFirstPresentationTitle1, R.string.onBoardingFirstPresentationDesc1),
                OnBoardingItems(R.drawable.onboarding_first_screen_2, R.string.onBoardingFirstPresentationTitle2, R.string.onBoardingFirstPresentationDesc2),
                OnBoardingItems(R.drawable.onboarding_first_screen_3, R.string.onBoardingFirstPresentationTitle3, R.string.onBoardingFirstPresentationDesc3),
                OnBoardingItems(R.drawable.onboarding_first_screen_4, R.string.onBoardingFirstPresentationTitle4, R.string.onBoardingFirstPresentationDesc4),
                OnBoardingItems(R.drawable.onboarding_first_screen_5, R.string.onBoardingFirstPresentationTitle5, R.string.onBoardingFirstPresentationDesc5),
                OnBoardingItems(R.drawable.onboarding_first_screen_6, R.string.onBoardingFirstPresentationTitle6, R.string.onBoardingFirstPresentationDesc6),
                OnBoardingItems(R.drawable.onboarding_first_screen_7, R.string.onBoardingFirstPresentationTitle7, R.string.onBoardingFirstPresentationDesc7)
            )
        }
        fun getDataUpdatePresentation(): List<OnBoardingItems>{
            return listOf(
                OnBoardingItems(R.drawable.onboarding_update_time_zone, R.string.onBoardingUpdatePresentationTitle1, R.string.onBoardingUpdatePresentationDesc1),
                OnBoardingItems(R.drawable.onboarding_update_total_time, R.string.onBoardingUpdatePresentationTitle2, R.string.onBoardingUpdatePresentationDesc2),
            )
        }
    }
}