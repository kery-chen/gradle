/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.language.swift.plugins

import org.gradle.internal.os.OperatingSystem
import org.gradle.language.swift.SwiftExecutable
import org.gradle.language.swift.tasks.SwiftCompile
import org.gradle.nativeplatform.tasks.InstallExecutable
import org.gradle.nativeplatform.tasks.LinkExecutable
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import spock.lang.Specification

class SwiftExecutablePluginTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def projectDir = tmpDir.createDir("project")
    def project = ProjectBuilder.builder().withProjectDir(projectDir).withName("testApp").build()

    def "adds extension with convention for source layout and module name"() {
        given:
        def src = projectDir.file("src/main/swift/main.swift").createFile()

        when:
        project.pluginManager.apply(SwiftExecutablePlugin)

        then:
        project.executable instanceof SwiftExecutable
        project.executable.module.get() == "TestApp"
        project.executable.swiftSource.files == [src] as Set
    }

    def "registers a component for the executable"() {
        when:
        project.pluginManager.apply(SwiftExecutablePlugin)

        then:
        project.components.main == project.executable
    }

    def "adds compile, link and install tasks"() {
        given:
        def src = projectDir.file("src/main/swift/main.swift").createFile()

        when:
        project.pluginManager.apply(SwiftExecutablePlugin)

        then:
        def compileSwift = project.tasks.compileSwift
        compileSwift instanceof SwiftCompile
        compileSwift.source.files == [src] as Set
        compileSwift.objectFileDirectory.get().asFile == projectDir.file("build/main/objs")

        def link = project.tasks.linkMain
        link instanceof LinkExecutable
        link.binaryFile.get().asFile == projectDir.file("build/exe/" + OperatingSystem.current().getExecutableName("TestApp"))

        def install = project.tasks.installMain
        install instanceof InstallExecutable
        install.installDirectory.get().asFile == projectDir.file("build/install/TestApp")
        install.runScript.name == OperatingSystem.current().getScriptName("TestApp")
    }

    def "output file names are calculated from module name defined on extension"() {
        when:
        project.pluginManager.apply(SwiftExecutablePlugin)
        project.executable.module = "App"

        then:
        def compileSwift = project.tasks.compileSwift
        compileSwift.moduleName == "App"

        def link = project.tasks.linkMain
        link.binaryFile.get().asFile == projectDir.file("build/exe/" + OperatingSystem.current().getExecutableName("App"))

        def install = project.tasks.installMain
        install.installDirectory.get().asFile == projectDir.file("build/install/App")
        install.runScript.name == OperatingSystem.current().getScriptName("App")
    }
}
