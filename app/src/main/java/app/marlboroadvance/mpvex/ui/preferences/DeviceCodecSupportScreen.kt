package app.marlboroadvance.mpvex.ui.preferences

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.marlboroadvance.mpvex.R
import app.marlboroadvance.mpvex.presentation.Screen
import app.marlboroadvance.mpvex.ui.preferences.components.CodecGroupCard
import app.marlboroadvance.mpvex.ui.preferences.components.DeviceCodecSummaryCard
import app.marlboroadvance.mpvex.ui.preferences.components.rememberDeviceCodecReport
import app.marlboroadvance.mpvex.ui.utils.LocalBackStack
import app.marlboroadvance.mpvex.ui.utils.readableContentWidth
import app.marlboroadvance.mpvex.utils.media.DeviceCodecReport
import kotlinx.serialization.Serializable

/**
 * Settings → Decoder → Device codec support: which video formats this phone decodes in
 * hardware, grouped into common formats, 10-bit and HDR, and older formats.
 */
@Serializable
object DeviceCodecSupportScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val backgroundColor = rememberPreferenceBackgroundColor()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val entries = rememberDeviceCodecReport()

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = backgroundColor,
      topBar = {
        PreferenceTopBar(
          title = {
            Text(
              text = stringResource(R.string.codec_support_title),
              style = MaterialTheme.typography.titleLargeEmphasized,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            IconButton(onClick = backstack::removeLastOrNull) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.secondary,
              )
            }
          },
          scrollBehavior = scrollBehavior,
          containerColor = backgroundColor,
        )
      },
    ) { padding ->
      if (entries == null) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
          LoadingIndicator(modifier = Modifier.size(48.dp))
        }
        return@Scaffold
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize().readableContentWidth(),
        contentPadding = PaddingValues(
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding() + 24.dp,
          start = 8.dp,
          end = 8.dp,
        ),
      ) {
        item { DeviceCodecSummaryCard(entries) }

        val groups = listOf(
          DeviceCodecReport.Group.Common to R.string.codec_support_group_common,
          DeviceCodecReport.Group.TenBitHdr to R.string.codec_support_group_10bit,
          DeviceCodecReport.Group.Older to R.string.codec_support_group_older,
        )
        groups.forEach { (group, titleRes) ->
          val inGroup = entries.filter { it.group == group }
          if (inGroup.isNotEmpty()) {
            item(key = "header_$group") { PreferenceSectionHeader(title = stringResource(titleRes)) }
            item(key = "group_$group") { CodecGroupCard(inGroup) }
          }
        }
      }
    }
  }
}
