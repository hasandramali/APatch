package com.valvesoftware.aq.ui.screen

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.InstallScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PatchesDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.valvesoftware.aq.APApplication
import com.valvesoftware.aq.Natives
import com.valvesoftware.aq.R
import com.valvesoftware.aq.apApp
import com.valvesoftware.aq.ui.component.ConfirmResult
import com.valvesoftware.aq.ui.component.KPModuleRemoveButton
import com.valvesoftware.aq.ui.component.LoadingDialogHandle
import com.valvesoftware.aq.ui.component.ProvideMenuShape
import com.valvesoftware.aq.ui.component.rememberConfirmDialog
import com.valvesoftware.aq.ui.component.rememberLoadingDialog
import com.valvesoftware.aq.ui.viewmodel.KPModel
import com.valvesoftware.aq.ui.viewmodel.KPModuleViewModel
import com.valvesoftware.aq.ui.viewmodel.PatchesViewModel
import com.valvesoftware.aq.util.inputStream
import com.valvesoftware.aq.util.ui.APDialogBlurBehindUtils
import com.valvesoftware.aq.util.writeTo
import java.io.IOException

private const val TAG = "KernelPatchModule"
private lateinit var targetKPMToControl: KPModel.KPMInfo

@Destination<RootGraph>
@Composable
fun KPModuleScreen(navigator: DestinationsNavigator) {
    val state by APApplication.apStateLiveData.observeAsState(APApplication.State.UNKNOWN_STATE)
    if (state == APApplication.State.UNKNOWN_STATE) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text(
                    text = stringResource(id = R.string.kpm_kp_not_installed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    val viewModel = viewModel<KPModuleViewModel>()

    LaunchedEffect(Unit) {
        if (viewModel.moduleList.isEmpty() || viewModel.isNeedRefresh) {
            viewModel.fetchModuleList()
        }
    }

    val kpModuleListState = rememberLazyListState()

    Scaffold(topBar = {
        TopBar()
    }, floatingActionButton = run {
        {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            val moduleLoad = stringResource(id = R.string.kpm_load)
            val moduleInstall = stringResource(id = R.string.kpm_install)
            val moduleEmbed = stringResource(id = R.string.kpm_embed)
            val successToastText = stringResource(id = R.string.kpm_load_toast_succ)
            val failToastText = stringResource(id = R.string.kpm_load_toast_failed)
            val loadingDialog = rememberLoadingDialog()

            val selectZipLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                Log.i(TAG, "select zip result: $uri")

                navigator.navigate(InstallScreenDestination(uri, MODULE_TYPE.KPM))
            }

            val selectKpmLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode != RESULT_OK) {
                    return@rememberLauncherForActivityResult
                }
                val data = it.data ?: return@rememberLauncherForActivityResult
                val uri = data.data ?: return@rememberLauncherForActivityResult

                // todo: args
                scope.launch {
                    val rc = loadModule(loadingDialog, uri, "") == 0
                    val toastText = if (rc) successToastText else failToastText
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, toastText, Toast.LENGTH_SHORT
                        ).show()
                    }
                    viewModel.markNeedRefresh()
                    viewModel.fetchModuleList()
                }
            }

            var expanded by remember { mutableStateOf(false) }
            val options = listOf(moduleEmbed, moduleInstall, moduleLoad)

            Column {
                FloatingActionButton(
                    onClick = {
                        expanded = !expanded
                    },
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.package_import),
                        contentDescription = null
                    )
                }

                ProvideMenuShape(RoundedCornerShape(10.dp)) {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        properties = PopupProperties(focusable = true)
                    ) {
                        options.forEach { label ->
                            DropdownMenuItem(text = { Text(label) }, onClick = {
                                expanded = false
                                when (label) {
                                    moduleEmbed -> {
                                        navigator.navigate(PatchesDestination(PatchesViewModel.PatchMode.PATCH_AND_INSTALL))
                                    }

                                    moduleInstall -> {
//                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
//                                        intent.type = "application/zip"
//                                        selectZipLauncher.launch(intent)
                                        Toast.makeText(
                                            context,
                                            "Under development",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    moduleLoad -> {
                                        val intent = Intent(Intent.ACTION_GET_CONTENT)
                                        intent.type = "*/*"
                                        selectKpmLauncher.launch(intent)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }
    }) { innerPadding ->

        KPModuleList(
            viewModel = viewModel,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            state = kpModuleListState
        )
    }
}

suspend fun loadModule(loadingDialog: LoadingDialogHandle, uri: Uri, args: String): Int {
    val rc = loadingDialog.withLoading {
        withContext(Dispatchers.IO) {
            run {
                val kpmDir: ExtendedFile =
                    FileSystemManager.getLocal().getFile(apApp.filesDir.parent, "kpm")
                kpmDir.deleteRecursively()
                kpmDir.mkdirs()
                val rand = (1..4).map { ('a'..'z').random() }.joinToString("")
                val kpm = kpmDir.getChildFile("${rand}.kpm")
                Log.d(TAG, "save tmp kpm: ${kpm.path}")
                var rc = -1
                try {
                    uri.inputStream().buffered().writeTo(kpm)
                    rc = Natives.loadKernelPatchModule(kpm.path, args).toInt()
                } catch (e: IOException) {
                    Log.e(TAG, "Copy kpm error: $e")
                }
                Log.d(TAG, "load ${kpm.path} rc: $rc")
                rc
            }
        }
    }
    return rc
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KPMControlDialog(showDialog: MutableState<Boolean>) {
    var controlParam by remember { mutableStateOf("") }
    var enable by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()
    val context = LocalContext.current
    val outMsgStringRes = stringResource(id = R.string.kpm_control_outMsg)
    val okStringRes = stringResource(id = R.string.kpm_control_ok)
    val failedStringRes = stringResource(id = R.string.kpm_control_failed)

    lateinit var controlResult: Natives.KPMCtlRes

    suspend fun onModuleControl(module: KPModel.KPMInfo) {
        loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                controlResult = Natives.kernelPatchModuleControl(module.name, controlParam)
            }
        }

        if (controlResult.rc >= 0) {
            Toast.makeText(
                context,
                "$okStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "$failedStringRes\n${outMsgStringRes}: ${controlResult.outMsg}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    BasicAlertDialog(
        onDismissRequest = { showDialog.value = false }, properties = DialogProperties(
            decorFitsSystemWindows = true,
            usePlatformDefaultWidth = false,
        )
    ) {
        Surface(
            modifier = Modifier
                .width(310.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(30.dp),
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            Column(modifier = Modifier.padding(PaddingValues(all = 24.dp))) {
                Box(
                    Modifier
                        .padding(PaddingValues(bottom = 16.dp))
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.kpm_control_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                Box(
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = stringResource(id = R.string.kpm_control_dialog_content),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Box(
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OutlinedTextField(
                        value = controlParam,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        onValueChange = {
                            controlParam = it
                            enable = controlParam.isNotBlank()
                        },
                        shape = RoundedCornerShape(50.0f),
                        label = { Text(stringResource(id = R.string.kpm_control_paramters)) },
                        visualTransformation = VisualTransformation.None,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }

                    Button(onClick = {
                        showDialog.value = false

                        scope.launch { onModuleControl(targetKPMToControl) }

                    }, enabled = enable) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
        val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
        APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun KPModuleList(
    viewModel: KPModuleViewModel, modifier: Modifier = Modifier, state: LazyListState
) {
    val moduleStr = stringResource(id = R.string.kpm)
    val moduleUninstallConfirm = stringResource(id = R.string.kpm_unload_confirm)
    val uninstall = stringResource(id = R.string.kpm_unload)
    val cancel = stringResource(id = android.R.string.cancel)

    val confirmDialog = rememberConfirmDialog()
    val loadingDialog = rememberLoadingDialog()

    val showKPMControlDialog = remember { mutableStateOf(false) }
    if (showKPMControlDialog.value) {
        KPMControlDialog(showDialog = showKPMControlDialog)
    }

    suspend fun onModuleUninstall(module: KPModel.KPMInfo) {
        val confirmResult = confirmDialog.awaitConfirm(
            moduleStr,
            content = moduleUninstallConfirm.format(module.name),
            confirm = uninstall,
            dismiss = cancel
        )
        if (confirmResult != ConfirmResult.Confirmed) {
            return
        }

        val success = loadingDialog.withLoading {
            withContext(Dispatchers.IO) {
                Natives.unloadKernelPatchModule(module.name) == 0L
            }
        }

        if (success) {
            viewModel.fetchModuleList()
        }
    }

    PullToRefreshBox(
        modifier = modifier,
        onRefresh = { viewModel.fetchModuleList() },
        isRefreshing = viewModel.isRefreshing
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = remember {
                PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + 16.dp + 56.dp /*  Scaffold Fab Spacing + Fab container height */
                )
            },
        ) {
            when {
                viewModel.moduleList.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.kpm_apm_empty), textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    items(viewModel.moduleList) { module ->
                        val scope = rememberCoroutineScope()
                        KPModuleItem(
                            module,
                            onUninstall = {
                                scope.launch { onModuleUninstall(module) }
                            },
                            onControl = {
                                targetKPMToControl = module
                                showKPMControlDialog.value = true
                            },
                        )

                        // fix last item shadow incomplete in LazyColumn
                        Spacer(Modifier.height(1.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar() {
    TopAppBar(title = { Text(stringResource(R.string.kpm)) })
}

@Composable
private fun KPModuleItem(
    module: KPModel.KPMInfo,
    onUninstall: (KPModel.KPMInfo) -> Unit,
    onControl: (KPModel.KPMInfo) -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val moduleAuthor = stringResource(id = R.string.kpm_author)
    val moduleArgs = stringResource(id = R.string.kpm_args)
    val decoration = TextDecoration.None

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(20.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(all = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .alpha(alpha = alpha)
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = module.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            textDecoration = decoration,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${module.version}, $moduleAuthor ${module.author}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = decoration,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "$moduleArgs: ${module.args}",
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = decoration,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                }

                Text(
                    modifier = Modifier
                        .alpha(alpha = alpha)
                        .padding(horizontal = 16.dp),
                    text = module.description,
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = decoration,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    FilledTonalButton(
                        onClick = { onControl(module) },
                        enabled = true,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.settings),
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.kpm_control),
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    KPModuleRemoveButton(enabled = true, onClick = { onUninstall(module) })
                }
            }

        }
    }
}
