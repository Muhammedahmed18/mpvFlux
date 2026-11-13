@file:Suppress("DEPRECATION")

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.marlboroadvance.mpvex.R

@SuppressLint("UseKtx")
@Composable
fun PermissionDeniedState(
  onRequestPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var showExplanationDialog by remember { mutableStateOf(false) }


  // Theme roles, so the first screen a new user sees matches their chosen theme,
  // light/dark mode and dynamic color (it used to be a fixed dark palette).
  val colors = MaterialTheme.colorScheme
  val backgroundColor = colors.surface
  val outerCircleColor = colors.surfaceContainerHigh
  val innerCircleColor = colors.primaryContainer

  Box(
    modifier = modifier.fillMaxSize().background(backgroundColor)
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
      // Custom Multi-layered Icon
      Box(
        contentAlignment = Alignment.Center,
        // Static icon: the old endless pulse was decorative motion with no meaning.
      ) {
        // Outer Circle
        Box(
          modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(outerCircleColor)
        )
        // Inner Circle
        Box(
          modifier = Modifier
            .size(130.dp)
            .clip(CircleShape)
            .background(innerCircleColor)
        )
        // Icon
        Icon(
          imageVector = Icons.Filled.FolderSpecial,
          contentDescription = null,
          modifier = Modifier.size(64.dp),
          tint = colors.onPrimaryContainer,
        )
      }

      Spacer(modifier = Modifier.height(48.dp))

      // Title
      Text(
        text = stringResource(R.string.storage_access_required_title),
        style = MaterialTheme.typography.headlineLargeEmphasized,
        textAlign = TextAlign.Center,
        color = colors.onSurface,
        lineHeight = 40.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Description
      Text(
        text = stringResource(R.string.storage_access_required_message),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        lineHeight = 24.sp
      )

      Spacer(modifier = Modifier.height(64.dp))

      // Grant Access Button
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
            .height(56.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
          containerColor = colors.primary,
          contentColor = colors.onPrimary,
        )
      ) {
        Text(
          text = stringResource(R.string.grant_access),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Why is this needed?
      TextButton(
        onClick = { showExplanationDialog = true },
        modifier = Modifier.padding(vertical = 8.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = colors.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.why_is_this_needed),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
          fontWeight = FontWeight.Normal,
        )
      }

      Spacer(modifier = Modifier.height(32.dp))
    }
  }

  // Explanation Dialog
  if (showExplanationDialog) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/Muhammedahmed18/mpvFlux"

    AlertDialog(
      onDismissRequest = { showExplanationDialog = false },
      icon = {
        Icon(
          imageVector = Icons.Outlined.Info,
          contentDescription = null,
          modifier = Modifier.size(24.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      title = {
        Text(text = stringResource(R.string.perm_why_title),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Medium,
        )
      },
      text = {
        Column(
          modifier =
            Modifier
              .heightIn(max = 400.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          // Standard build explanation
          Text(text = stringResource(R.string.perm_why_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(text = stringResource(R.string.perm_why_policy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(text = stringResource(R.string.perm_why_scope),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          Text(text = stringResource(R.string.perm_why_open_source),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          // Clickable GitHub link
          val annotatedString =
            buildAnnotatedString {
              pushStringAnnotation(
                tag = "URL",
                annotation = githubUrl,
              )
              withStyle(
                style =
                  SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline,
                  ),
              ) {
                append(githubUrl)
              }
              pop()
            }

          ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            onClick = { offset ->
              annotatedString
                .getStringAnnotations(
                  tag = "URL",
                  start = offset,
                  end = offset,
                ).firstOrNull()
                ?.let {
                  uriHandler.openUri(it.item)
                }
            },
          )

          Text(text = stringResource(R.string.perm_why_privacy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
          )
        }
      },
      confirmButton = {
        FilledTonalButton(
          onClick = { showExplanationDialog = false },
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.height(40.dp),
        ) {
          Text(stringResource(R.string.got_it))
        }
      },
      shape = MaterialTheme.shapes.large,
    )
  }
}
