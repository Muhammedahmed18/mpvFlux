package app.marlboroadvance.mpvex.ui.browser.states

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R

// ExperimentalMaterial3ExpressiveApi is already opted in project-wide via build.gradle.kts.
@SuppressLint("UseKtx")
@Composable
fun PermissionDeniedState(
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showExplanationDialog by remember { mutableStateOf(false) }

  // Theme roles, so the first screen a new user sees matches their chosen theme,
  // light/dark mode and dynamic color.
  val colors = MaterialTheme.colorScheme

  Box(
    modifier = modifier.fillMaxSize().background(colors.surface)
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(horizontal = 32.dp)
          .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Hero: expressive cookie shape with a large icon (static, no decorative motion)
      Box(contentAlignment = Alignment.Center) {
        Box(
          modifier = Modifier
            .size(200.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(colors.primaryContainer)
        )
        Icon(
          imageVector = Icons.Filled.FolderSpecial,
          contentDescription = null,
          modifier = Modifier.size(88.dp),
          tint = colors.onPrimaryContainer,
        )
      }

      Spacer(modifier = Modifier.height(40.dp))

      // Title
      Text(
        text = stringResource(R.string.storage_access_required_title),
        style = MaterialTheme.typography.headlineLargeEmphasized,
        textAlign = TextAlign.Center,
        color = colors.onSurface,
        lineHeight = 40.sp
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Description
      Text(
        text = stringResource(R.string.storage_access_required_message),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
      )

      Spacer(modifier = Modifier.height(48.dp))

      // Grant Access Button (large, shape morphs on press)
      Button(
        onClick = {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
              val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
              intent.data = Uri.parse("package:${context.packageName}")
              context.startActivity(intent)
            } catch (_: Exception) {
              val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
              context.startActivity(intent)
            }
          } else {
            onRequestPermission()
          }
        },
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.LargeContainerHeight),
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.LargeContainerHeight),
      ) {
        Text(
          text = stringResource(R.string.grant_access),
          style = MaterialTheme.typography.titleLargeEmphasized,
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Why is this needed?
      FilledTonalButton(
        onClick = { showExplanationDialog = true },
        modifier =
          Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        shapes = ButtonDefaults.shapes(),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
        Text(
          text = stringResource(R.string.why_is_this_needed),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  // Explanation Dialog
  if (showExplanationDialog) {
    val githubUrl = "https://github.com/MpvFlux/mpvFlux"

    AlertDialog(
      onDismissRequest = { showExplanationDialog = false },
      icon = {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
        }
      },
      title = {
        Text(
          text = stringResource(R.string.perm_why_title),
          style = MaterialTheme.typography.headlineSmall,
        )
      },
      text = {
        Column(
          modifier =
            Modifier
              .heightIn(max = 400.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = stringResource(R.string.perm_why_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(
            text = stringResource(R.string.perm_why_policy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(
            text = stringResource(R.string.perm_why_scope),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(
            text = stringResource(R.string.perm_why_open_source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          // Clickable GitHub link (LinkAnnotation opens via the default UriHandler)
          val linkColor = MaterialTheme.colorScheme.primary
          Text(
            text =
              buildAnnotatedString {
                withLink(
                  LinkAnnotation.Url(
                    url = githubUrl,
                    styles =
                      TextLinkStyles(
                        style =
                          SpanStyle(
                            color = linkColor,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.Underline,
                          ),
                      ),
                  ),
                ) {
                  append(githubUrl)
                }
              },
            style = MaterialTheme.typography.bodyMedium,
          )

          Text(
            text = stringResource(R.string.perm_why_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
        }
      },
      confirmButton = {
        FilledTonalButton(
          onClick = { showExplanationDialog = false },
          shapes = ButtonDefaults.shapes(),
        ) {
          Text(stringResource(R.string.got_it))
        }
      },
      shape = MaterialTheme.shapes.extraLarge,
    )
  }
}
