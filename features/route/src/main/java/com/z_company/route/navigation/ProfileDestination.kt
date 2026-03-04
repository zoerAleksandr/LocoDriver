package com.z_company.route.navigation

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.vk.id.onetap.compose.onetap.OneTap
import com.z_company.domain.navigation.Router
import com.z_company.route.R
import com.z_company.shared.ui.screen.ProfileScreen
import com.z_company.shared.viewmodel.ProfileSharedViewModel
import org.koin.compose.koinInject

@Composable
fun ProfileDestination(
    router: Router,
) {
    ProfileScreen(
        onBackClick = router::back,
        onPurchasesClick = router::showPurchasesScreen,
        vkAuthContent = { isLogin ->
            val viewModel: ProfileSharedViewModel = koinInject()
            OneTap(
                modifier = Modifier.fillMaxWidth(),
                onAuth = { _, accessToken ->
                    if (isLogin) {
                        viewModel.authWithVkId(accessToken.userID.toString())
                    } else {
                        viewModel.registerWithEmail(
                            accessToken.userData.email ?: "",
                            ""
                        )
                    }
                },
                onAuthCode = { _, _ -> },
                onFail = { _, vkIdAuthFail ->
                    Log.d("ProfileDestination", "VK auth failed: ${vkIdAuthFail.description}")
                },
                signInAnotherAccountButtonEnabled = true,
            )
        },
        vkProfileContent = {
            val viewModel: ProfileSharedViewModel = koinInject()
            OneTap(
                modifier = Modifier.fillMaxWidth(),
                onAuth = { _, accessToken ->
                    viewModel.attachVkId(accessToken.userID.toString())
                },
                onAuthCode = { _, _ -> },
                onFail = { _, vkIdAuthFail ->
                    Log.d("ProfileDestination", "VK attach failed: ${vkIdAuthFail.description}")
                },
                signInAnotherAccountButtonEnabled = true,
            )
        },
        policyContent = { isLicenseAccepted, onLicenseChanged, isPersonalDataAccepted, onPersonalDataChanged ->
            val ctx = LocalContext.current
            val hintStyle = MaterialTheme.typography.bodyMedium

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp)
            ) {
                // License agreement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = isLicenseAccepted,
                        onCheckedChange = { onLicenseChanged(!isLicenseAccepted) }
                    )
                    val urlLicense = stringResource(id = R.string.url_to_license_agreement)
                    val text = "Я принимаю условия Лицензионного соглашения"
                    val startIndex = 19
                    val annotationString = buildAnnotatedString {
                        append(text)
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = startIndex, end = text.length
                        )
                    }
                    ClickableText(
                        text = annotationString,
                        style = hintStyle.copy(color = MaterialTheme.colorScheme.primary)
                    ) {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, urlLicense.toUri()))
                    }
                }

                // Personal data processing policy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = isPersonalDataAccepted,
                        onCheckedChange = { onPersonalDataChanged(!isPersonalDataAccepted) }
                    )
                    val urlPolicy = stringResource(id = R.string.url_to_personal_data_processing_policy)
                    val text = "Я ознакомлен с политикой обработки персональных данных"
                    val startIndex = 15
                    val annotationString = buildAnnotatedString {
                        append(text)
                        addStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.tertiary,
                                textDecoration = TextDecoration.Underline
                            ),
                            start = startIndex, end = text.length
                        )
                    }
                    ClickableText(
                        text = annotationString,
                        style = hintStyle.copy(color = MaterialTheme.colorScheme.primary)
                    ) {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, urlPolicy.toUri()))
                    }
                }
            }
        },
    )
}
