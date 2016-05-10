# PullToRefresh源码解析

-------

## 1. 架构设计

解耦刷新头部和刷新内容.刷新内容可以是任何View,刷新头部和刷新内容通过自定义的ViewGroup进行布局.
对于刷新头部,抽象出功能接口和UI接口.其中: 

* PtrHandler: 下拉刷新的功能接口,包含判断是否能刷新的方法和真正刷新工作方法.
* PtrUIHandler: 下拉刷新的UI接口,包括准备下拉,下拉中,下拉过程中位置变化,下拉完成,重置这5个回调方法.通常情况下,HeaderView需要实现这个接口,来处理下拉刷新过程中UI的变化.

整个项目的核心类是PtrFrameLayout,它代表了下拉刷新的自定义控件集合,包含了下拉头部(Header View)和刷新内容(Content View)这两个子View.

## 2. 源码讲解

### PtrHandler.java

下拉刷新的功能接口,源码如下:
```java
/**
 * 下拉刷新接口抽象
 */
public interface PtrHandler {
    /**
     * 判断当前View是否能刷新,UI层面的控制.(例如:ListView和ScrollView是否处于顶部)
     */
    boolean checkCanDoRefresh(final PtrFrameLayout frame, final View content, final View header);

    /**
     * 刷新回调函数
     */
    void onRefreshBegin(final PtrFrameLayout frame);
}
```
其中,checkCanDoRefresh是UI层面的控制,例如Content View是ListView且当前处于ListView的中间,这个时候checkCanDoRefresh需要返回false，因为用户下拉的操作应该交给ListView执行而不是交给PtrFrameLayout进行下拉刷新操作.

### PtrDefaultHandler.java

PtrDefaultHandler虽然是抽象类,但是它实现了PtrHandler定义的checkCanDoRefresh方法,具体实现如下：
```java
/**
 * PtrHandler接口的默认实现类.
 */
public abstract class PtrDefaultHandler implements PtrHandler {
    public static boolean canChildScrollUp(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;

                return absListView.getChildCount() > 0 &&
                        (absListView.getFirstVisiblePosition() > 0 ||
                        absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return view.getScrollY() > 0;
            }
        } else {
            return view.canScrollVertically(-1);
        }
    }

    @Override
    public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
        return !canChildScrollUp(content);
    }
}
```