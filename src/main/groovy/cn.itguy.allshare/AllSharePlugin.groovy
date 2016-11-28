package cn.itguy.allshare

import com.tonicsystems.jarjar.Main
import org.gradle.api.Plugin
import org.gradle.api.Project

class AllSharePlugin implements Plugin<Project> {

    static final String TAG = "AllSharePlugin";

    @Override
    void apply(Project project) {
        project.afterEvaluate {
            project.android.applicationVariants.each { variant ->
                def dexTaskName = "transformClassesWithDexFor${variant.name.capitalize()}"
                def dexTask = project.tasks.findByName(dexTaskName)
                if (dexTask) {
                    def replaceWXEntryActivityBeforeDex = "replaceWXEntryActivityBeforeDex${variant.name.capitalize()}"
                    // 新建task
                    project.task(replaceWXEntryActivityBeforeDex) << {
                        replaceWXEntryActivity(project, variant, dexTask);
                    }
                    //insert task
                    def replaceWXEntryActivityBeforeDexTask = project.tasks[replaceWXEntryActivityBeforeDex]
                    replaceWXEntryActivityBeforeDexTask.dependsOn dexTask.taskDependencies.getDependencies(dexTask)
                    dexTask.dependsOn replaceWXEntryActivityBeforeDexTask
                } else {
                    println("$TAG: not found task:$dexTaskName}, replace failed.")
                }
            }

        }
    }

    static void replaceWXEntryActivity(def project, def variant, def dexTask) {
        println("$TAG: variant=${variant.name.capitalize()}")
        println("$TAG: variant applicationId=$variant.mergedFlavor.applicationId")

        Set<File> inputFiles = dexTask.inputs.files.files
        inputFiles.each { inputFile ->
            def path = inputFile.absolutePath
//            println("$TAG: inputFile path=$path")

            if (path.contains("allshare") && path.endsWith("jars/classes.jar")) {
                println("$TAG: processJar=$inputFile.absolutePath")
                def ruleFile = new File(inputFile.getParent(), "rule.txt")
                PrintWriter rulePW = new PrintWriter(ruleFile)
                rulePW.println("rule cn.itguy.allshare.platform.wx.WXEntryActivity ${variant.mergedFlavor.applicationId}.wxapi.WXEntryActivity")
                rulePW.flush()
                rulePW.close()

                def outputFile = new File(inputFile.getParent(), inputFile.name + ".opt")
                println("$TAG: outputFile=${outputFile.absolutePath}")

                new Main().process(ruleFile, inputFile, outputFile)

                if (inputFile.exists()) {
                    inputFile.delete()
                }

                println("$TAG: new jar exists=${inputFile.exists()}")

                outputFile.renameTo(inputFile)

                println("$TAG: new jar exists=${inputFile.exists()}")
            }

        }
    }

}