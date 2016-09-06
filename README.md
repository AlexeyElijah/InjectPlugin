# InjectPlugin
Android studio 插件工程
修改butterknife zeleny源码,自动生成注解代码,不用依赖别的库
使用方法:右键点击java文件中的layout文件名,点击Generate KNife injections,既可生成注解代码
生成的格式例子为:(不能生成BindViews)

@BindView(R.id.tv_main)
TextView tvMain

@OnClick(R.id.tv_container)
    public void onClick() {
  }
  
  并且在方法中会生成
 BKnife.inject(this);以便调用代码
