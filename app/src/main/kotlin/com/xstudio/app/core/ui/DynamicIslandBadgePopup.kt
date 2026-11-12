package com.xstudio.app.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.data.model.UserRole

fun UserRole.badgeIcon(): ImageVector = when (this) {
    UserRole.OWNER -> Icons.Default.Diamond
    UserRole.DEVELOPER -> Icons.Default.Code
    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
    UserRole.BETA_TESTER -> Icons.Default.Science
    UserRole.VERIFIED -> Icons.Default.Verified
    UserRole.PREMIUM -> Icons.Default.AutoAwesome
    UserRole.PRO -> Icons.Default.Bolt
    UserRole.FREE -> Icons.Default.Person
}

@Composable
fun RoleBadgeChip(
    role: UserRole,
    onClick: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(XWhite.copy(alpha = 0.12f))
            .border(1.dp, XGlassBorder, RoundedCornerShape(50))
            .clickable { onClick(role) }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = role.badgeIcon(),
            contentDescription = role.displayName,
            tint = XWhite,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = role.displayName.uppercase(),
            color = XWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun DynamicIslandBadgePopup(
    selectedRole: UserRole?,
    onDismiss: () -> Unit
) {
    if (selectedRole == null) return

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                initialScale = 0.6f
            ) + expandVertically(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .widthIn(min = 200.dp, max = 280.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(XBlack)
                    .border(1.5.dp, XWhite.copy(alpha = 0.35f), RoundedCornerShape(32.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = selectedRole.badgeIcon(),
                        contentDescription = selectedRole.displayName,
                        tint = XWhite,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = selectedRole.displayName.uppercase(),
                        color = XWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedRole.description,
                        color = XWhiteMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
