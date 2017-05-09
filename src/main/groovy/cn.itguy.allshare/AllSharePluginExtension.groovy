package cn.itguy.allshare

/**
 * Created by yelongfei490 on 2017/3/11.
 */
class AllSharePluginExtension {

    /**
     * 插件扩展参数，qq平台的{package, appId}映射
     */
    public Map<String, String> qqAppIdMap;

    /**
     * 插件扩展参数，微信平台的{package, appId}映射
     */
    public Map<String, String> wxAppIdMap;


    @Override
    public String toString() {
        return "AllSharePluginExtension{" +
                "qqAppIdMap=" + qqAppIdMap +
                ", wxAppIdMap=" + wxAppIdMap +
                '}';
    }
}
