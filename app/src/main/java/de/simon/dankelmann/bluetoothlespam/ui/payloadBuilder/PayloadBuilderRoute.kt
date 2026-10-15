package de.simon.dankelmann.bluetoothlespam.ui.payloadBuilder

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import de.simon.dankelmann.bluetoothlespam.BleSpamApplication
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AdvertisementSetListEntity
import de.simon.dankelmann.bluetoothlespam.Database.Entities.AssociationListSetEntity
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetList
import de.simon.dankelmann.bluetoothlespam.R

private const val LOG_TAG = "PayloadBuilderRoute"

/** Route entry point — owns the business logic for the custom payload builder screen. */
@Composable
fun PayloadBuilderRoute(
    onNavigateToAdvertisement: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: PayloadBuilderViewModel = viewModel()

    val title by viewModel.title.observeAsState(viewModel.title.value!!)
    val manufacturerId by viewModel.manufacturerId.observeAsState(viewModel.manufacturerId.value!!)
    val payloadHex by viewModel.payloadHex.observeAsState(viewModel.payloadHex.value!!)
    val txPowerLevel by viewModel.txPowerLevel.observeAsState(viewModel.txPowerLevel.value!!)
    val isSaving by viewModel.isSaving.observeAsState(false)
    val saveMessageResId by viewModel.saveMessageResId.observeAsState()

    saveMessageResId?.let { resId ->
        LaunchedEffect(resId) {
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.saveMessageResId.value = null
        }
    }

    PayloadBuilderScreen(
        title = title,
        onTitleChanged = { viewModel.title.value = it },
        manufacturerId = manufacturerId,
        manufacturerIdError = !viewModel.isManufacturerIdValid(),
        onManufacturerIdChanged = { viewModel.manufacturerId.value = it },
        payloadHex = payloadHex,
        payloadHexError = viewModel.payloadHexError(),
        payloadByteCount = viewModel.payloadByteCount(),
        onPayloadHexChanged = { viewModel.payloadHex.value = it },
        txPowerLevel = txPowerLevel,
        onTxPowerLevelChanged = { viewModel.txPowerLevel.value = it },
        canSubmit = viewModel.isInputValid(),
        isSaving = isSaving,
        onAdvertiseNowClicked = { advertiseCustomPayload(context, viewModel, onNavigateToAdvertisement) },
        onSaveClicked = { saveCustomPayload(context, viewModel) },
    )
}

/**
 * "Advertise now": wraps the single generated set in its own list/collection and pushes it
 * through the app's normal advertising path (`AdvertisementSetQueueHandler.activate`), the same
 * sequence the collection/quick-start flows use, then lands on the advertisement screen.
 */
private fun advertiseCustomPayload(
    context: Context,
    viewModel: PayloadBuilderViewModel,
    onNavigateToAdvertisement: () -> Unit,
) {
    val set = viewModel.buildAdvertisementSet() ?: return

    val list = AdvertisementSetList().apply {
        title = set.title
        advertisementSets.add(set)
    }
    val collection = AdvertisementSetCollection().apply {
        title = set.title
        advertisementSetLists.add(list)
    }

    val app = context.applicationContext as BleSpamApplication
    app.queueHandler.apply {
        deactivate(context)
        setAdvertisementSetCollection(collection)
        activate(context)
    }
    onNavigateToAdvertisement()
}

/**
 * Persists the set via [DatabaseHelpers.saveAdvertisementSet] and attaches it to the user
 * list "Custom Payloads" (created on first use) so it shows up as a regular saved set.
 * Room work stays off the main thread.
 */
private fun saveCustomPayload(context: Context, viewModel: PayloadBuilderViewModel) {
    val set = viewModel.buildAdvertisementSet() ?: return
    val listTitle = context.getString(R.string.pb_list_title)

    viewModel.isSaving.value = true
    Thread {
        try {
            val setId = DatabaseHelpers.saveAdvertisementSet(set)
            attachSetToNamedList(setId, listTitle)
            viewModel.isSaving.postValue(false)
            viewModel.saveMessageResId.postValue(R.string.pb_saved)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Saving custom payload failed", e)
            viewModel.isSaving.postValue(false)
            viewModel.saveMessageResId.postValue(R.string.pb_save_failed)
        }
    }.start()
}

private fun attachSetToNamedList(setId: Int, listTitle: String) {
    val database = AppDatabase.getInstance()
    val existing = database.advertisementSetListDao().getAll().firstOrNull { it.title == listTitle }
    val listId = existing?.id
        ?: database.advertisementSetListDao()
            .insertItem(AdvertisementSetListEntity(id = 0, title = listTitle))
            .toInt()

    val position = database.associationListSetDao().findByListId(listId).size
    database.associationListSetDao().insertItem(
        AssociationListSetEntity(
            id = 0,
            advertisementSetId = setId,
            advertisementSetListId = listId,
            position = position,
        ),
    )
}
