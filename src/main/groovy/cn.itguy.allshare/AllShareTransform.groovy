package cn.itguy.allshare

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.google.common.collect.Sets
import com.tonicsystems.jarjar.Main
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project
import sun.instrument.TransformerManager

import java.util.jar.JarFile

/**
 * Created by yelongfei490 on 2017/5/8.
 */
class AllShareTransform extends Transform {

    public static final String TAG = "AllShareTransform";

    Project project

    public AllShareTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return 'AllShare'
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return Sets.immutableEnumSet(
//                QualifiedContent.Scope.PROJECT,
//                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
//                QualifiedContent.Scope.SUB_PROJECTS,
//                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES)
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        // 根据transformInvocation.context从而得到applicationId
        def contextPath = transformInvocation.context.path
        def variantCapitalize = contextPath.substring(contextPath.lastIndexOf('For') + 3)
        def android = project.extensions.findByType(AppExtension)
        ApplicationVariant currentVariant = android.applicationVariants.find {
            it.name.capitalize() == variantCapitalize
        }
        log(currentVariant.name)
        def applicationId = currentVariant.mergedFlavor.applicationId
        log(applicationId)

        Collection<TransformInput> inputs = transformInvocation.getInputs()
        inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                // 重名名输出文件,因为可能同名,会覆盖
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath);
                String destName = jarInput.name;
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }
                // 获得输出文件
                File dest = transformInvocation.outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR);

                if (!dest.exists()) {
                    dest.getParentFile().mkdirs()
                    dest.createNewFile()
                }

                log(dest.absolutePath)

                // 处理jar
                File newFile = processJar(jarInput.file, applicationId)
                log("outputFile exists? ${newFile.exists()}")

                FileUtils.copyFile(newFile, dest);
            }
        }
    }


    private static File processJar(File file, def applicationId) {
        println("$TAG: processJar=$file.absolutePath")
        def ruleFile = new File(file.getParent(), "rule.txt")
        PrintWriter rulePW = new PrintWriter(ruleFile)
        rulePW.println("rule cn.itguy.allshare.platform.wx.WXEntryActivity ${applicationId}.wxapi.WXEntryActivity")
        rulePW.flush()
        rulePW.close()

        def outputFile = new File(file.getParent(), file.name + ".opt")
        println("$TAG: outputFile=${outputFile.absolutePath}")

        new Main().process(ruleFile, file, outputFile)
        return outputFile
    }

    void log(String msg) {
        project.logger.error("------${msg}")
    }

}
