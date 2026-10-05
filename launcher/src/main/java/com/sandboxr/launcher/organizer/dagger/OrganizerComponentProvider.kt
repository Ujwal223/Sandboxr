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

package com.sandboxr.launcher.organizer.dagger

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.sandboxr.launcher.model.repository.AppsListRepository
import com.sandboxr.launcher.model.repository.HomeScreenRepository
import com.sandboxr.launcher.organizer.OrganizerTransactionContext
import com.sandboxr.launcher.organizer.creation.screen.ui.foldercreator.FolderCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.spacecreator.SpaceCreatorViewModel
import com.sandboxr.launcher.organizer.creation.screen.ui.workspaceorganizer.WorkspaceOrganizerViewModel
import com.sandboxr.launcher.organizer.generator.CreationSession
import com.sandboxr.launcher.organizer.generator.DefaultTopicProvider
import com.sandboxr.launcher.organizer.generator.FolderCreationSession
import com.sandboxr.launcher.organizer.generator.FolderPlacer
import com.sandboxr.launcher.organizer.generator.HeuristicScreenPlacer
import com.sandboxr.launcher.organizer.generator.PackageManagerItemInfoClassifier
import com.sandboxr.launcher.organizer.generator.PresetTemplateGenerator
import com.sandboxr.launcher.organizer.generator.ScreenCreationSession
import javax.inject.Provider

/**
 * Interface implemented by Application instances that expose custom [OrganizerComponent].
 */
interface OrganizerComponentProviderOwner {
    val organizerComponent: OrganizerComponent
}

/**
 * Default implementation of [OrganizerComponent].
 */
class DefaultOrganizerComponent(
    private val appContext: Context,
    private val appsListRepository: AppsListRepository = AppsListRepository(),
    private val homeScreenRepository: HomeScreenRepository = HomeScreenRepository()
) : OrganizerComponent {

    private val topicProvider = DefaultTopicProvider()
    private val classifier = PackageManagerItemInfoClassifier(appContext)
    private val templateGenerator = PresetTemplateGenerator()
    private val folderPlacer = FolderPlacer()
    private val screenPlacer = HeuristicScreenPlacer()

    private val folderSession = FolderCreationSession(
        appsListRepository = appsListRepository,
        classifier = classifier,
        topicProvider = topicProvider,
        folderPlacer = folderPlacer
    )

    private val screenSession = ScreenCreationSession(
        appsListRepository = appsListRepository,
        classifier = classifier,
        topicProvider = topicProvider,
        templateGenerator = templateGenerator,
        placer = screenPlacer
    )

    private val creationSessionFactory = CreationSession.Factory(
        folderCreationSessionProvider = Provider { folderSession },
        screenCreationSessionProvider = Provider { screenSession }
    )

    private val transactionContext = OrganizerTransactionContext(
        modelWriter = null,
        homeScreenRepository = homeScreenRepository
    )

    override fun getFolderCreatorViewModel(): FolderCreatorViewModel {
        return FolderCreatorViewModel(
            creationSession = folderSession,
            transactionContext = transactionContext
        )
    }

    override fun getWorkspaceOrganizerViewModel(): WorkspaceOrganizerViewModel {
        return WorkspaceOrganizerViewModel(
            transactionContext = transactionContext,
            homeScreenRepository = homeScreenRepository
        )
    }

    override fun getSpaceCreatorViewModel(): SpaceCreatorViewModel {
        return SpaceCreatorViewModel(
            creationSession = screenSession,
            transactionContext = transactionContext
        )
    }

    override fun getOrganizerTransactionContext(): OrganizerTransactionContext {
        return transactionContext
    }

    override fun getCreationSessionFactory(): CreationSession.Factory {
        return creationSessionFactory
    }
}

/**
 * Provider helper to retrieve or override [OrganizerComponent].
 */
object OrganizerComponentProvider {
    @Volatile
    private var testComponent: OrganizerComponent? = null

    @JvmStatic
    fun get(context: Context): OrganizerComponent {
        testComponent?.let { return it }

        val app = context.applicationContext
        if (app is OrganizerComponentProviderOwner) {
            return app.organizerComponent
        }

        return DefaultOrganizerComponent(app)
    }

    @VisibleForTesting
    @JvmStatic
    fun setTestComponent(component: OrganizerComponent?) {
        testComponent = component
    }
}
