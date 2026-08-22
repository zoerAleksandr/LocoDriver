package com.z_company.route.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.z_company.domain.navigation.Router
import com.z_company.route.component.PdfActionSheet
import com.z_company.route.ui.StatisticsScreen
import com.z_company.route.util.StatisticsPdfGenerator
import com.z_company.route.viewmodel.StatisticsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StatisticsDestination(
    router: Router,
) {
    val viewModel: StatisticsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }

    StatisticsScreen(
        state = state,
        onBack = router::back,
        onTab = viewModel::setTab,
        onPrev = viewModel::prevPeriod,
        onNext = viewModel::nextPeriod,
        onCompareSelect = viewModel::setCompare,
        onOpenDetail = viewModel::openDetail,
        onCloseDetail = viewModel::closeDetail,
        onSelectDetailMonth = viewModel::selectDetailMonth,
        onPickBaselineMonth = viewModel::setCompareCustomMonth,
        onPickBaselineYear = viewModel::setCompareCustomYear,
        onSelectHistoryMetric = viewModel::selectHistoryMetric,
        isExportingPdf = exporting,
        onExportPdf = {
            if (exporting || state.loading) return@StatisticsScreen
            exporting = true
            scope.launch {
                try {
                    val file = withContext(Dispatchers.IO) {
                        StatisticsPdfGenerator(context).generate(state)
                    }
                    val authority = "${context.packageName}.fileprovider"
                    pdfUri = FileProvider.getUriForFile(context, authority, file)
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка формирования PDF: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    exporting = false
                }
            }
        },
    )

    pdfUri?.let { uri ->
        PdfActionSheet(uri = uri, onDismiss = { pdfUri = null })
    }
}
