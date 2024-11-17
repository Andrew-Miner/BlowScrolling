package com.example.blowscrolling.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.blowscrolling.R


@Composable
fun VoteGroup(selection: Int? = null, onUpVoteClick: ((Boolean) -> Int)? = null, onDownVoteClick: ((Boolean) -> Int)? = null, modifier: Modifier = Modifier) {
    var internalSelection by rememberSaveable { selection?.let { mutableIntStateOf(selection) } ?: run { mutableIntStateOf(0) } }
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        IconToggleButton(
            checked = (selection == 1 || (selection == null && internalSelection == 1)),
            onCheckedChange = {
                if(selection != null && onUpVoteClick != null)
                    internalSelection = onUpVoteClick(it)
                else {
                    if (!it) {
                        internalSelection = 0
                    }
                    else {
                        internalSelection = 1
                    }
                }
            }
        ) {
            if (selection == 1 || (selection == null && internalSelection == 1)) {
                Icon(
                    painter = painterResource(id = R.drawable.up_arrow_filled),
                    contentDescription = "Localized Description",
                    tint = Color.Unspecified,
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.up_arrow),
                    contentDescription = "Localized Description",
                    //tint = Color.Unspecified
                )
            }
        }

        IconToggleButton(
            checked = (selection == 2 || (selection == null && internalSelection == 2)),
            onCheckedChange = {
                if(selection != null && onDownVoteClick != null)
                    internalSelection = onDownVoteClick(it)
                else {
                    if (!it) {
                        internalSelection = 0
                    }
                    else {
                        internalSelection = 2
                    }
                }
            }
        ) {
            if (selection == 2 || selection == null && internalSelection == 2) {
                Icon(
                    painter = painterResource(id = R.drawable.down_arrow_filled),
                    contentDescription = "Localized Description",
                    tint = Color.Unspecified
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.down_arrow),
                    contentDescription = "Localized Description",
                    //tint = Color.Unspecified
                )
            }
        }
    }
}