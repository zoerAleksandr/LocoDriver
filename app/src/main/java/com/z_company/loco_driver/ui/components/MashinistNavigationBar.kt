package com.z_company.loco_driver.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z_company.loco_driver.ui.theme.InterFont
import com.z_company.loco_driver.ui.theme.MashinistTheme

enum class NavTab { HOME, SALARY, ADD, SETTINGS, PROFILE }

@Composable
fun MashinistNavigationBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MashinistTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
                .align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem("Главный", "🏠", selectedTab == NavTab.HOME) {
                onTabSelected(NavTab.HOME)
            }
            NavItem("Зарплата", "₽", selectedTab == NavTab.SALARY) {
                onTabSelected(NavTab.SALARY)
            }
            Box(
                modifier = Modifier
                    .size(56.dp, 32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable { onTabSelected(NavTab.ADD) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    color = colors.accentInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            NavItem("Настройки", "⚙", selectedTab == NavTab.SETTINGS) {
                onTabSelected(NavTab.SETTINGS)
            }
            NavItem("Профиль", "👤", selectedTab == NavTab.PROFILE) {
                onTabSelected(NavTab.PROFILE)
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    iconChar: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MashinistTheme.colors
    val tint = if (selected) colors.accent else colors.textFaint
    Column(
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (selected)
                        Modifier
                            .background(colors.accentSoft, RoundedCornerShape(999.dp))
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    else Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = iconChar,
                color = tint,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontFamily = InterFont,
        )
    }
}
