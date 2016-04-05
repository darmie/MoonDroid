Toast = luajava.bindClass 'android.widget.Toast'

class MoonToast
  new: =>
    print AndroidContext
    print Toast
    text = "Hello toast!"
    toast = Toast\makeText(AndroidContext, text, Toast.LENGTH_LONG)
    toast\show()

return MoonToast!
