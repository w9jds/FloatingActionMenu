# FloatingActionMenu [![Build Status](https://travis-ci.com/w9jds/FloatingActionMenu.svg?token=1b4pt5U1oA46nUYTBosj&branch=master)](https://travis-ci.com/w9jds/FloatingActionMenu)

Floating Action Menu Android Library, Built specifically on top of the Design support library `FloatingActionButton`.

Currently the library is quite limited, but hopefully it will evolve to much more. To include this library in your project, add jitpack using this:

``` gradle
 repositories {
    jcenter()
    maven { url "https://jitpack.io" }
 }
 ```

Now add this into your dependency section in your gradle file.

``` gradle
compile 'com.github.w9jds:FloatingActionMenu:master-SNAPSHOT'
```

Using the library is extremely easy. There is only one element inside of the library, which you then place all of you buttons inside. The following are all of the custom attributes for the element, and what they do:

```
base_src = reference to drawable used on main FAB (default is + sign)
base_background = color to use on main FAB
base_ripple = color to use as ripple on main FAB
base_marginEnd = margin to use on the end of the entire menu (recommended = 8dp)
base_marginBottom = margin to use on the bottom of the menu (recommended = 8dp)
overlay_color = color used on the overlay displayed when the menu is open
item_spacing = spacing between each item in the menu (recommended = 16dp)
```

Useage should look something like this:

``` xml
<com.w9jds.FloatingActionMenu
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:base_src="@drawable/ic_positive"
    app:base_background="@color/colorAccent"
    app:base_marginEnd="8dp"
    app:base_marginBottom="8dp"
    app:overlay_color="#66000000"
    app:item_spacing="8dp">

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/scan_item"
        app:backgroundTint="@android:color/white"
        android:src="@drawable/ic_qr_code"
        android:layout_height="wrap_content"
        android:layout_width="wrap_content"
        android:contentDescription="Scan Qr Code" />

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/search_item"
        app:backgroundTint="@android:color/white"
        android:src="@drawable/ic_search"
        android:layout_height="wrap_content"
        android:layout_width="wrap_content"
        android:contentDescription="Search" />

</com.w9jds.FloatingActionMenu>
```
