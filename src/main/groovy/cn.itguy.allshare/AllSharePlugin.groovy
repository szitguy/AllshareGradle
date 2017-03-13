package cn.itguy.allshare

import com.tonicsystems.jarjar.Main
import org.gradle.api.Plugin
import org.gradle.api.Project

class AllSharePlugin implements Plugin<Project> {

    static final String TAG = "AllSharePlugin";

    @Override
    void apply(Project project) {
        // 初始化AllSharePlugin扩展参数
        project.extensions.create(TAG, AllSharePluginExtension);

        project.afterEvaluate {
            def extension = project.extensions.findByName(TAG) as AllSharePluginExtension
            def qqAppId = extension.qqAppId
            println("$TAG: Get qqAppId: $qqAppId.")
//            // 将qqAppId注入android的defaultConfig的manifestholder中。***此方法行不通，资源文件（manifest、res等）合并前，必须执行完单个module的build.gradle中的变量替代***
//            project.android.defaultConfig.manifestPlaceholders.put("QQ_APP_ID", qqAppId)

            // 改成以res/string的形式注入qqAppId
            project.android.defaultConfig.resValue("string", "qqAppId", "tencent$qqAppId")

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

    static File findJar(File inputFile) {
        File returnFile = null;
        println("$TAG: findJar---file name=${inputFile.name}")
        if (inputFile.isDirectory()) {
            inputFile.listFiles().each { file ->
                File tempFile = findJar(file)
                if (tempFile != null) {
                    returnFile = tempFile
                }
            }
        } else {
            if (inputFile.name.endsWith(".jar")) {
                returnFile = inputFile
            }
        }

        return returnFile;
    }

    static void replaceWXEntryActivity(def project, def variant, def dexTask) {
        println("$TAG: variant=${variant.name.capitalize()}")
        println("$TAG: variant applicationId=$variant.mergedFlavor.applicationId")

        Set<File> inputFiles = dexTask.inputs.files.files
        inputFiles.each { inputFile ->
            def path = inputFile.absolutePath
            println("$TAG: inputFile path=$path")

            // 经过打印观察到，在用版本的gradle插件打包时执行dexTask的输入文件有两个，一个是存放了所有类混淆后的jar包的目录，目录中有个jar包，另一个是个文件，maindexlist.txt记录了要dex处理的class列表
            // 那么这里我们要做的就是遍历得到的目录中的所有文件，找到jar文件，然后将WXEntryActivity的包名替换过来
            if (inputFile.isDirectory()) {
                File file = findJar(inputFile)
                if (null != file) {
                    println("$TAG: processJar=$file.absolutePath")
                    def ruleFile = new File(file.getParent(), "rule.txt")
                    PrintWriter rulePW = new PrintWriter(ruleFile)
                    rulePW.println("rule cn.itguy.allshare.platform.wx.WXEntryActivity ${variant.mergedFlavor.applicationId}.wxapi.WXEntryActivity")
                    rulePW.flush()
                    rulePW.close()

                    def outputFile = new File(file.getParent(), file.name + ".opt")
                    println("$TAG: outputFile=${outputFile.absolutePath}")

                    new Main().process(ruleFile, file, outputFile)

                    if (file.exists()) {
                        file.delete()
                    }

                    println("$TAG: new jar exists=${file.exists()}")

                    outputFile.renameTo(file)

                    println("$TAG: new jar exists=${file.exists()}")
                } else {
                    println("$TAG: jar not found.")
                }
            }
        }
    }

}