package com.exhibitiondot.presentation.ui.screen.main.home

sealed interface HomeUiState {
    data object Nothing : HomeUiState

    data object ShowSearchDialog : HomeUiState

    data object ShowRegionFilter : HomeUiState

    data object ShowCategoryFilter : HomeUiState

    data object ShowEventTypeFilter : HomeUiState
}