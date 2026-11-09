package my.noveldokusha.coreui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import my.noveldokusha.coreui.R
import my.noveldokusha.coreui.theme.NovelaRadius
import my.noveldokusha.coreui.theme.NovelaSize
import my.noveldokusha.coreui.theme.NovelaTheme

enum class ToolbarMode { MAIN, SEARCH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarSearch(
    focusRequester: FocusRequester,
    searchTextInput: String,
    onSearchTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onTextDone: (String) -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    placeholderText: String = stringResource(R.string.search_here),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    inputEnabled: Boolean = true,
    labelText: String? = null,
    modifier: Modifier = Modifier,
    showMenuButton: Boolean = false,
    onMenuClick: (() -> Unit)? = null,
    dropdownContent: @Composable (() -> Unit)? = null,
) {
    val colors = NovelaTheme.colors
    val keyboardController = LocalSoftwareKeyboardController.current
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            if (inputEnabled) focusRequester.requestFocus()
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    keyboardController?.hide()
                    onClose()
                },
                modifier = Modifier.padding(start = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                )
            }
        },
        title = {
            Surface(
                shape = RoundedCornerShape(NovelaRadius.pill),
                color = colors.surfaceSubtle,
                border = BorderStroke(
                    width = if (isFocused) 1.25.dp else NovelaSize.hairline,
                    color = if (isFocused) colors.accent else colors.borderSubtle,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp)
                    .height(NovelaSize.searchBarHeight),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = if (isFocused) colors.accent else colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (searchTextInput.isEmpty()) {
                            Text(
                                text = labelText ?: placeholderText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = searchTextInput,
                            onValueChange = onSearchTextChange,
                            enabled = inputEnabled,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary,
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    if (searchTextInput.isNotBlank()) {
                                        onTextDone(searchTextInput)
                                    }
                                },
                                onDone = {
                                    keyboardController?.hide()
                                    if (searchTextInput.isNotBlank()) {
                                        onTextDone(searchTextInput)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (!focusState.isFocused) {
                                        keyboardController?.hide()
                                    }
                                },
                        )
                    }
                    AnimatedVisibility(
                        visible = searchTextInput.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.borderSubtle,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    onSearchTextChange("")
                                    if (inputEnabled) focusRequester.requestFocus()
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                    if (searchTextInput.isEmpty() && showMenuButton) {
                        Box {
                            IconButton(
                                onClick = {
                                    if (dropdownContent != null) {
                                        dropdownExpanded = !dropdownExpanded
                                    } else {
                                        onMenuClick?.invoke()
                                    }
                                },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                            ) {
                                dropdownContent?.invoke()
                            }
                        }
                    }
                }
            }
        }
    )
}
