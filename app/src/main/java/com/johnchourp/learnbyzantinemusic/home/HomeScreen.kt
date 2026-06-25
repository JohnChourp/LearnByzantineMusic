package com.johnchourp.learnbyzantinemusic.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.johnchourp.learnbyzantinemusic.ui.components.StaggeredAppear
import com.johnchourp.learnbyzantinemusic.ui.theme.LbmPageBg

/**
 * Redesigned home screen: an animated hero header followed by grouped, icon-led
 * navigation sections and a version footer. Pure UI — all navigation is supplied via
 * the [HomeTile.onClick] lambdas in [sections].
 */
@Composable
fun HomeScreen(
    title: String,
    subtitle: String,
    version: String,
    sections: List<HomeSection>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LbmPageBg)
            .verticalScroll(scrollState),
    ) {
        HomeHeroHeader(title = title, subtitle = subtitle)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            var appearIndex = 0
            sections.forEach { section ->
                StaggeredAppear(delayMillis = appearDelay(appearIndex++)) {
                    SectionHeader(
                        title = stringResource(section.titleRes),
                        subtitle = section.subtitleRes?.let { stringResource(it) },
                    )
                }
                if (section.info == HomeInfo.QuantityVoices) {
                    StaggeredAppear(delayMillis = appearDelay(appearIndex++)) {
                        QuantityVoicesInfoCard()
                    }
                }
                section.tiles.forEach { tile ->
                    StaggeredAppear(delayMillis = appearDelay(appearIndex++)) {
                        NavTile(tile = tile, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            PoweredByFooter(version = version)
        }
    }
}

/** Per-item entrance delay, capped so the last items don't lag noticeably. */
private fun appearDelay(index: Int): Int = (index * 45).coerceAtMost(540)
