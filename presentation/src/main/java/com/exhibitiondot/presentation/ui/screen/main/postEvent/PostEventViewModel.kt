package com.exhibitiondot.presentation.ui.screen.main.postEvent

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.exhibitiondot.domain.model.Category
import com.exhibitiondot.domain.model.EventInfo
import com.exhibitiondot.domain.model.EventType
import com.exhibitiondot.domain.model.ImageSource
import com.exhibitiondot.domain.model.Region
import com.exhibitiondot.domain.usecase.event.AddEventUseCase
import com.exhibitiondot.domain.usecase.event.GetEventDetailUseCase
import com.exhibitiondot.domain.usecase.event.UpdateEventUseCase
import com.exhibitiondot.presentation.base.BaseViewModel
import com.exhibitiondot.presentation.mapper.DateFormatStrategy
import com.exhibitiondot.presentation.mapper.format
import com.exhibitiondot.presentation.mapper.getMessage
import com.exhibitiondot.presentation.model.GlobalFlagModel
import com.exhibitiondot.presentation.model.GlobalUiModel
import com.exhibitiondot.presentation.ui.navigation.MainScreen
import com.exhibitiondot.presentation.ui.state.EditTextState
import com.exhibitiondot.presentation.ui.state.MultiFilterState
import com.exhibitiondot.presentation.ui.state.SingleFilterState
import com.exhibitiondot.presentation.util.ImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PostEventViewModel @Inject constructor(
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val getEventDetailUseCase: GetEventDetailUseCase,
    private val imageProcessor: ImageProcessor,
    private val uiModel: GlobalUiModel,
    private val flagModel: GlobalFlagModel,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel() {
    private val eventId = savedStateHandle.toRoute<MainScreen.PostEvent>().eventId
    val editMode = eventId != null

    var uiState by mutableStateOf<PostEventUiState>(PostEventUiState.Idle)
        private set
    var currentStep by mutableStateOf(PostEventStep.UploadImage)
        private set
    private val totalSteps = PostEventStep.entries

    var image by mutableStateOf<ImageSource?>(null)
        private set
    val nameState = EditTextState(maxLength = 20)
    var selectedDate by mutableStateOf(format(DateFormatStrategy.Today))
        private set
    val regionState = SingleFilterState(Region.entries)
    val categoryState = MultiFilterState(Category.entries)
    val eventTypeState = MultiFilterState(EventType.entries)

    init {
        eventId?.let {
            setEventInfo(it)
        }
    }

    private fun setEventInfo(eventId: Long) {
        viewModelScope.launch {
            getEventDetailUseCase(eventId)
                .onSuccess { eventDetail ->
                    with(eventDetail) {
                        nameState.typeText(name)
                        selectedDate = date
                        regionState.setFilter(region)
                        categoryState.setFilter(categoryList)
                        eventTypeState.setFilter(eventTypeList)
                        image = ImageSource.Remote(imgUrl)
                    }
                }
        }
    }

    fun onPhotoPickerResult(uri: Uri?) {
        if (uri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = imageProcessor.compressUriToFile(uri, 420, 560)
                withContext(Dispatchers.Main) {
                    result
                        .onSuccess { file ->
                            image = ImageSource.Local(file)
                        }
                        .onFailure {
                            uiModel.showToast("이미지 변환에 실패했어요")
                        }
                }
            }
        }
    }

    fun deleteImage() {
        val selectedImage = image
        if (selectedImage != null) {
            image = null
            deleteFile(selectedImage)
        }
    }

    private fun deleteFile(imageSource: ImageSource) {
        if (imageSource is ImageSource.Local) {
            viewModelScope.launch(Dispatchers.IO) {
                imageSource.file.delete()
            }
        }
    }

    fun showDatePicker() {
        uiState = PostEventUiState.ShowDatePicker(selectedDate)
    }

    fun dismiss() {
        uiState = PostEventUiState.Idle
    }

    fun onDateSelect(date: String) {
        selectedDate = date
    }

    fun onPrevStep(onBack: () -> Unit) {
        val prevIdx = totalSteps.indexOf(currentStep) - 1
        if (prevIdx < 0) {
            image?.let { deleteFile(it) }
            onBack()
        } else {
            currentStep = totalSteps[prevIdx]
        }
    }

    fun onNextStep(moveEventDetail: (Long) -> Unit, onBack: () -> Unit) {
        val nextIdx = totalSteps.indexOf(currentStep) + 1
        if (nextIdx > totalSteps.lastIndex) {
            postingEvent(moveEventDetail, onBack)
        } else {
            currentStep = totalSteps[nextIdx]
        }
    }

    fun validate(): Boolean {
        return when (currentStep) {
            PostEventStep.UploadImage -> image != null
            PostEventStep.EventInfo -> nameState.isValidate() &&
                    selectedDate.isNotEmpty() &&
                    regionState.selectedFilter != null &&
                    categoryState.selectedFilterList.isNotEmpty() &&
                    eventTypeState.selectedFilterList.isNotEmpty()
        }
    }

    fun lastStep(): Boolean {
        return currentStep == totalSteps.last()
    }

    private fun postingEvent(moveEventDetail: (Long) -> Unit, onBack: () -> Unit) {
        uiState = PostEventUiState.Loading
        viewModelScope.launch {
            val eventInfo = EventInfo(
                name = nameState.trimmedText(),
                date = selectedDate,
                region = regionState.selectedFilter!!,
                categoryList = categoryState.selectedFilterList,
                eventTypeList = eventTypeState.selectedFilterList,
            )
            if (editMode) {
                updateEvent(eventId!!, eventInfo, moveEventDetail)
            } else {
                addEvent(eventInfo, onBack)
            }
        }
    }

    private suspend fun addEvent(eventInfo: EventInfo, onBack: () -> Unit) {
        val selectedImage = image as ImageSource.Local
        addEventUseCase(selectedImage, eventInfo)
            .onSuccess {
                flagModel.setHomeUpdateFlag(true)
                showMessage("이벤트를 추가했어요")
                deleteFile(selectedImage)
                onBack()
            }
            .onFailure { t ->
                val msg = t.getMessage("이벤트 추가에 실패했어요")
                showMessage(msg)
            }
    }

    private suspend fun updateEvent(
        eventId: Long,
        eventInfo: EventInfo,
        moveEventDetail: (Long) -> Unit
    ) {
        val selectedImage = image!!
        updateEventUseCase(selectedImage, eventInfo, eventId)
            .onSuccess {
                flagModel.setHomeUpdateFlag(true)
                showMessage("이벤트를 수정했어요")
                deleteFile(selectedImage)
                moveEventDetail(eventId)
            }
            .onFailure { t ->
                val msg = t.getMessage("이벤트 수정에 실패했어요")
                showMessage(msg)
            }
    }

    private fun showMessage(msg: String) {
        uiState = PostEventUiState.Idle
        uiModel.showToast(msg)
    }
}