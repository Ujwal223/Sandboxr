/*
 * Copyright (C) 2026 The Android Open Source Project
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.organizer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.lifecycle.Lifecycle
import com.sandboxr.launcher.LauncherSettings.Favorites
import com.sandboxr.launcher.model.data.AppInfo
import com.sandboxr.launcher.model.data.AppsListData
import com.sandboxr.launcher.model.data.FolderInfo
import com.sandboxr.launcher.model.data.WorkspaceItemInfo
import com.sandboxr.launcher.model.repository.AppsListRepository
import com.sandboxr.launcher.organizer.creation.screen.ui.OrganizerActivity
import com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreatorActivity
import com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.SpaceCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceOrganizerViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceScreenData
import com.sandboxr.launcher.organizer.dagger.OrganizerComponent
import com.sandboxr.launcher.organizer.dagger.OrganizerComponentProvider
import com.sandboxr.launcher.organizer.generator.CreationSession
import com.sandboxr.launcher.organizer.generator.DefaultTopicProvider
import com.sandboxr.launcher.organizer.generator.FolderCreationSession
import com.sandboxr.launcher.organizer.generator.FolderPlacer
import com.sandboxr.launcher.organizer.generator.HeuristicScreenPlacer
import com.sandboxr.launcher.organizer.generator.ItemInfoClassifier
import com.sandboxr.launcher.organizer.generator.PresetTemplateGenerator
import com.sandboxr.launcher.organizer.generator.ScreenCreationSession
import com.sandboxr.launcher.organizer.generator.TopicClassifiedItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import javax.inject.Provider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrganizerSystemTest {

    private lateinit var context: Context
    private lateinit var transactionContext: OrganizerTransactionContext
    private lateinit var appsListRepository: AppsListRepository

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        transactionContext = OrganizerTransactionContext()
        appsListRepository = AppsListRepository()

        val mockClassifier = object : ItemInfoClassifier {
            override suspend fun classify(
                items: List<com.sandboxr.launcher.model.data.ItemInfo>,
                topics: List<String>
            ): List<TopicClassifiedItem> {
                return items.mapIndexed { index, item ->
                    val topic = topics.getOrElse(index % topics.size) { "General" }
                    TopicClassifiedItem(item, topic, 1.0f)
                }
            }
        }

        val topicProvider = DefaultTopicProvider()
        val templateGenerator = PresetTemplateGenerator()
        val folderPlacer = FolderPlacer()
        val screenPlacer = HeuristicScreenPlacer()

        val folderSession = FolderCreationSession(
            appsListRepository = appsListRepository,
            classifier = mockClassifier,
            topicProvider = topicProvider,
            folderPlacer = folderPlacer
        )

        val screenSession = ScreenCreationSession(
            appsListRepository = appsListRepository,
            classifier = mockClassifier,
            topicProvider = topicProvider,
            templateGenerator = templateGenerator,
            placer = screenPlacer
        )

        val creationSessionFactory = CreationSession.Factory(
            folderCreationSessionProvider = Provider { folderSession },
            screenCreationSessionProvider = Provider { screenSession }
        )

        val testComponent = object : OrganizerComponent {
            override fun getFolderCreatorViewModel(): FolderCreatorViewModel {
                return FolderCreatorViewModel(folderSession, transactionContext)
            }

            override fun getWorkspaceOrganizerViewModel(): WorkspaceOrganizerViewModel {
                return WorkspaceOrganizerViewModel(transactionContext)
            }

            override fun getSpaceCreatorViewModel(): SpaceCreatorViewModel {
                return SpaceCreatorViewModel(screenSession, transactionContext)
            }

            override fun getOrganizerTransactionContext(): OrganizerTransactionContext {
                return transactionContext
            }

            override fun getCreationSessionFactory(): CreationSession.Factory {
                return creationSessionFactory
            }
        }

        OrganizerComponentProvider.setTestComponent(testComponent)
    }

    @After
    fun tearDown() {
        OrganizerComponentProvider.setTestComponent(null)
    }

    @Test
    fun testOrganizerActivityLaunchesInWorkspaceMode() {
        val intent = Intent(context, OrganizerActivity::class.java).apply {
            putExtra(OrganizerActivity.EXTRA_MODE, OrganizerActivity.MODE_WORKSPACE)
        }
        val controller = Robolectric.buildActivity(OrganizerActivity::class.java, intent).setup()
        val activity = controller.get()

        assertNotNull(activity)
        assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
    }

    @Test
    fun testOrganizerActivityLaunchesInFolderAndSpaceModes() {
        val folderIntent = Intent(context, OrganizerActivity::class.java).apply {
            putExtra(OrganizerActivity.EXTRA_MODE, OrganizerActivity.MODE_FOLDERS)
        }
        val folderController = Robolectric.buildActivity(OrganizerActivity::class.java, folderIntent).setup()
        assertEquals(Lifecycle.State.RESUMED, folderController.get().lifecycle.currentState)

        val spaceIntent = Intent(context, OrganizerActivity::class.java).apply {
            putExtra(OrganizerActivity.EXTRA_MODE, OrganizerActivity.MODE_SPACE)
        }
        val spaceController = Robolectric.buildActivity(OrganizerActivity::class.java, spaceIntent).setup()
        assertEquals(Lifecycle.State.RESUMED, spaceController.get().lifecycle.currentState)
    }

    @Test
    fun testFolderCreatorActivityLaunchesSuccessfully() {
        val controller = Robolectric.buildActivity(FolderCreatorActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity)
        assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
    }

    @Test
    fun testFolderCreatorGeneratesAndCommitsOrganizedFolders() = runBlocking {
        val app1 = createAppInfo("com.sandboxr.chat", "Chat App")
        val app2 = createAppInfo("com.sandboxr.social", "Social Feed")
        val app3 = createAppInfo("com.sandboxr.work", "Docs Editor")

        appsListRepository.dispatchChange(AppsListData(arrayOf(app1, app2, app3), 0))

        val organizerComponent = OrganizerComponentProvider.get(context)
        val viewModel = organizerComponent.getFolderCreatorViewModel()

        ShadowLooper.idleMainLooper()

        // Verify topics are loaded
        val state = viewModel.state.value
        assertFalse("Topics should not be empty", state.topics.isEmpty())

        // Select topics
        viewModel.selectAll()
        ShadowLooper.idleMainLooper()
        assertTrue("All topics selected", viewModel.state.value.selectedTopics.isNotEmpty())

        // Generate folders
        viewModel.generateFolders()
        ShadowLooper.idleMainLooper()

        val genResult = viewModel.state.value.generatedFolders
        assertTrue("Generation result must be Folders", genResult is CreationSession.GenerationResult.Folders)
        val folders = (genResult as CreationSession.GenerationResult.Folders).folders
        assertFalse("Generated folders must not be empty", folders.isEmpty())

        // Verify folder contents
        for (folder in folders) {
            assertNotNull(folder.title)
            assertTrue("Folder should have placed items", folder.getContents().isNotEmpty())
        }

        // Commit folders to workspace
        var finishedCalled = false
        viewModel.commitFolders {
            finishedCalled = true
        }

        assertTrue("Commit finish callback invoked", finishedCalled)
        assertTrue("State marked complete", viewModel.state.value.isComplete)
        assertEquals("Folders added to transaction context", folders.size, transactionContext.addedFolders.size)
    }

    @Test
    fun testWorkspaceOrganizerRearrangesIcons() = runBlocking {
        val organizerComponent = OrganizerComponentProvider.get(context)
        val viewModel = organizerComponent.getWorkspaceOrganizerViewModel()

        ShadowLooper.idleMainLooper()

        val item1 = WorkspaceItemInfo().apply {
            id = 101
            title = "Test App 1"
            cellX = 0
            cellY = 0
            screenId = 0
            container = Favorites.CONTAINER_DESKTOP
        }
        val item2 = WorkspaceItemInfo().apply {
            id = 102
            title = "Test App 2"
            cellX = 1
            cellY = 0
            screenId = 0
            container = Favorites.CONTAINER_DESKTOP
        }

        val initialScreens = listOf(
            WorkspaceScreenData(screenId = 0, pageIndex = 0, items = listOf(item1, item2)),
            WorkspaceScreenData(screenId = 1, pageIndex = 1, items = emptyList())
        )
        viewModel.loadScreens(initialScreens)
        ShadowLooper.idleMainLooper()

        assertEquals(2, viewModel.state.value.screens.size)
        assertEquals(2, viewModel.state.value.screens[0].items.size)

        // Rearrange icon: move item1 from (0, 0) on screen 0 to (2, 3) on screen 0
        viewModel.moveItem(itemId = 101, fromScreenId = 0, toScreenId = 0, targetCellX = 2, targetCellY = 3)

        val movedItem = viewModel.state.value.screens[0].items.find { it.id == 101 }
        assertNotNull(movedItem)
        assertEquals("Target cellX rearranged", 2, movedItem!!.cellX)
        assertEquals("Target cellY rearranged", 3, movedItem.cellY)

        // Move item2 from screen 0 to screen 1 at (0, 0)
        viewModel.moveItem(itemId = 102, fromScreenId = 0, toScreenId = 1, targetCellX = 0, targetCellY = 0)

        assertEquals("Screen 0 now has 1 item", 1, viewModel.state.value.screens[0].items.size)
        assertEquals("Screen 1 now has 1 item", 1, viewModel.state.value.screens[1].items.size)
        val itemOnScreen1 = viewModel.state.value.screens[1].items.find { it.id == 102 }
        assertNotNull(itemOnScreen1)
        assertEquals("Screen 1 ID", 1, itemOnScreen1!!.screenId)

        // Commit changes
        var commitCalled = false
        viewModel.commitChanges {
            commitCalled = true
        }
        ShadowLooper.idleMainLooper()

        assertTrue("Changes committed", commitCalled)
        assertTrue("Saved flag set", viewModel.state.value.isSaved)
        assertEquals("Items persisted to transaction context", 2, transactionContext.createdScreens.size)
    }

    @Test
    fun testWorkspaceOrganizerReordersPages() = runBlocking {
        val organizerComponent = OrganizerComponentProvider.get(context)
        val viewModel = organizerComponent.getWorkspaceOrganizerViewModel()

        ShadowLooper.idleMainLooper()

        val screens = listOf(
            WorkspaceScreenData(screenId = 0, pageIndex = 0, items = emptyList()),
            WorkspaceScreenData(screenId = 1, pageIndex = 1, items = emptyList()),
            WorkspaceScreenData(screenId = 2, pageIndex = 2, items = emptyList())
        )
        viewModel.loadScreens(screens)
        ShadowLooper.idleMainLooper()

        // Reorder page 0 to index 2
        viewModel.reorderPages(fromIndex = 0, toIndex = 2)

        val updatedScreens = viewModel.state.value.screens
        assertEquals(1, updatedScreens[0].screenId)
        assertEquals(2, updatedScreens[1].screenId)
        assertEquals(0, updatedScreens[2].screenId)

        // Add a new screen
        viewModel.addScreen()
        assertEquals(4, viewModel.state.value.screens.size)

        // Commit
        viewModel.commitChanges()
        ShadowLooper.idleMainLooper()

        assertEquals(listOf(1, 2, 0, 3), transactionContext.lastPageOrder)
    }

    @Test
    fun testSpaceCreatorGeneratesAndCommitsSpaces() = runBlocking {
        val app1 = createAppInfo("com.sandboxr.tools", "Calculator")
        val app2 = createAppInfo("com.sandboxr.notes", "Notes")

        appsListRepository.dispatchChange(AppsListData(arrayOf(app1, app2), 0))

        val organizerComponent = OrganizerComponentProvider.get(context)
        val spaceViewModel = organizerComponent.getSpaceCreatorViewModel()

        ShadowLooper.idleMainLooper()

        spaceViewModel.selectAll()
        spaceViewModel.generateSpaces()
        ShadowLooper.idleMainLooper()

        val result = spaceViewModel.state.value.generatedResult
        assertTrue("Generated result is Screens", result is CreationSession.GenerationResult.Screens)
        val screens = (result as CreationSession.GenerationResult.Screens).pages
        assertFalse("Screens list should not be empty", screens.isEmpty())

        var spaceFinished = false
        spaceViewModel.commitSpaces {
            spaceFinished = true
        }
        ShadowLooper.idleMainLooper()

        assertTrue("Space commit callback invoked", spaceFinished)
        assertTrue("Space marked complete", spaceViewModel.state.value.isComplete)
        assertTrue("Transaction received space screens", transactionContext.createdScreens.isNotEmpty())
    }

    private fun createAppInfo(packageName: String, label: String): AppInfo {
        val component = ComponentName(packageName, "$packageName.MainActivity")
        val user = Process.myUserHandle()
        val intent = Intent().setComponent(component)
        return AppInfo(component, label, user, intent)
    }
}
