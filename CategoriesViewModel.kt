package ru.project.demo.view.catalog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.HttpException
import ru.project.demo.R
import ru.project.demo.data.model.UiError
import ru.project.demo.data.model.catalog.Category
import ru.project.demo.data.repository.CatalogRepository
import ru.project.demo.util.SingleLiveEvent
import ru.project.demo.util.isNetworkConnected
import ru.project.demo.view.base.BaseViewModel

class CategoriesViewModel(private val catalogRepository: CatalogRepository) : BaseViewModel() {
    private val _categoriesLiveData = MutableLiveData<List<Category>>()
    val categoriesLiveData: LiveData<List<Category>>
        get() = _categoriesLiveData

    private val _categoriesErrorEvent = SingleLiveEvent<UiError>()
    val categoriesErrorEvent: LiveData<UiError>
        get() = _categoriesErrorEvent

    private var isCategoriesLoading = false

    fun loadCategories() {
        if (isCategoriesLoading) return

        if (_categoriesLiveData.value != null) {
            return
        }

        if (!isNetworkConnected()) {
            _showNoConnectionEvent.call()
            return
        }
        _showLoadingEvent.value = true
        isCategoriesLoading = true
        disposableManager.add(
            catalogRepository.loadCategories()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.code==1) {
                        var presents = Category("", "Все подарки", null)
                        it.result?.add((it.result?.size),presents)
                        _categoriesLiveData.value = it.result
                    } else
                        _categoriesErrorEvent.value = UiError(R.string.error_loading_categories)

                    isCategoriesLoading = false
                    _showLoadingEvent.value = false
                }, {
                    handleCategoriesError(it)

                    isCategoriesLoading = false
                    _showLoadingEvent.value = false
                })
        )
    }

    private fun handleCategoriesError(throwable: Throwable) {
        if (throwable is HttpException) {
            when (throwable.code()) {
                401 -> _showAuthErrorEvent.call()
                400 -> _categoriesErrorEvent.value = UiError(R.string.error_incorrect_request_data)
                429 -> _categoriesErrorEvent.value = UiError(R.string.error_request_limit)
                else -> _categoriesErrorEvent.value = UiError(R.string.error_loading_categories)
            }
        } else {
            _categoriesErrorEvent.value = UiError(R.string.error_loading_categories)
        }
    }

    companion object {
        const val TAG = "CategoriesViewModel"
    }
}