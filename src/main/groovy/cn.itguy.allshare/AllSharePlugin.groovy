package cn.itguy.allshare

import cn.itguy.BuildConfig
import com.android.build.gradle.AppExtension
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

class AllSharePlugin implements Plugin<Project> {

    static final String TAG = "AllSharePlugin";

    @Override
    void apply(Project project) {
        // 注册transform
        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new AllShareTransform(project))

        // 初始化AllSharePlugin扩展参数
        project.extensions.create(TAG, AllSharePluginExtension)
        def extension = project.extensions.findByType(AllSharePluginExtension)

        // release时添加jitpack仓库
        if (!BuildConfig.DEBUG) {
//            project.repositories.add(project.getRepositories().jcenter())
//            // 添加AllShare Android库依赖
//            project.dependencies.add("compile", "cn.itguy.allshare:allshare:${BuildConfig.ANDROID_LIB_VERSION}")

            project.repositories.add(project.repositories.maven(new Action<MavenArtifactRepository>() {
                @Override
                void execute(MavenArtifactRepository mavenArtifactRepository) {
                    mavenArtifactRepository.url = 'https://jitpack.io'
                }
            }))
            // 添加AllShare Android库依赖
            project.dependencies.add("compile", "com.github.szitguy:Allshare:${BuildConfig.ANDROID_LIB_VERSION}")

            println("------ANDROID_LIB_VERSION is ${BuildConfig.ANDROID_LIB_VERSION}")
        }

        project.afterEvaluate {
            println("------${extension}")
            def qqAppIdMap = extension.qqAppIdMap
            def wxAppIdMap = extension.wxAppIdMap
//            // 将qqAppId注入android的defaultConfig的manifestholder中。***此方法行不通，资源文件（manifest、res等）合并前，必须执行完单个module的build.gradle中的变量替代***
//            project.android.defaultConfig.manifestPlaceholders.put("QQ_APP_ID", qqAppId)

            // 改成以res/string的形式注入qqAppId、wxAppId
            android.applicationVariants.each { variant ->
                def applicationId = variant.applicationId
                def qqAppId = qqAppIdMap[applicationId]
                def wxAppId = wxAppIdMap[applicationId]
                println("------${variant.name}(${applicationId}) use qqAppId: ${qqAppId}")
                println("------${variant.name}(${applicationId}) use wxAppId: ${wxAppId}")

                variant.resValue("string", "all_share_qq_app_id", "tencent${qqAppId}")
                variant.resValue("string", "all_share_wx_app_id", wxAppId)
            }
        }
    }

}