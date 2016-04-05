# MoonDroid
Develop Android apps with MoonScript and LuaJ


##Dependencies
1. [Luaj](http://tidal-loop.github.io/LuaJ)  -- Java Library to run the Lua VM
2. [LPEG/J](https://github.com/leonrd/lpegj) -- A Java port of LPEG , a Lua Parsing Expression Grammar library to be used by the MoonScript parser/compiler
3. [MoonScript Compiler](https://github.com/leafo/moonscript) 

##Usage
Create a file called `init.lua` and 
```lua
package.preload['lpeg'] = function (...)
    return require "org.moon.mooonlanding.lpeg"   -- this looks for the lpeg Java class in the project directory
end


--parse and execute  the `*.moon` files

activity = ...

local getScript = function(script)
  return activity:findResource(script)  --call findResource method from our android activity class
end

-- See MoonScript reference for how to use the compiler API

local moonscript = require("moonscript.base")
local parse = require("moonscript.parse")
local MoonApp = activity:readFile("init.moon") --initialize my moon app
local prs = parse.string(MoonApp)
prs()  -- parse the moon file
local run = moonscript.loadstring(MoonApp)
return run()
```


 The `init.moon` code, you can import android/java classes using the luajav API 

```moon
Toast = luajava.bindClass 'android.widget.Toast'

class MoonToast
  new: =>
    print AndroidContext
    print Toast
    text = "Hello toast!"
    toast = Toast\makeText(AndroidContext, text, Toast.LENGTH_LONG)
    toast\show()

return MoonToast!
```

The Android activity

```java
public class MoonActivity extends Activity implements ResourceFinder {
	public Globals lua;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LuaValue activity = CoerceJavaToLua.coerce(this); //Coerce Java object as a LuaValue
		LuaValue contxt = CoerceJavaToLua.coerce(this.getApplicationContext()); 
		lua = JsePlatform.standardGlobals();
		lua.finder = this;
		try{
			lua.set("AndroidContext", contxt); //Set activity context as a global LuaValue
			lua.load(new DebugLib());  //Load Luaj's version of Lua debub library, Moonscript and lpeg need this to work
			lua.loadfile("Moon.lua").call(activity); //load the Lua and run the Lua file and set 'activity' as a global value
		}catch(Exception e){
			e.printStackTrace();
			
		}
	}
	
	public void log (String msg){
		
			try {
				Log.d("MOONSCRIPT", msg);		
			} catch (Exception e) {
				e.printStackTrace();
				
			}
		
	}
	
	public String readFile(String path) {
		String text = "";
		//InputStream myfile = this.findResource(path);
		
			try {
				InputStream is = getAssets().open(path);
		        int size = is.available();
		        byte[] buffer = new byte[size];
		        is.read(buffer);
		        is.close();
		        text = new String(buffer);
		        
		        return text;
		    } catch (Exception e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
			return text;
	}
	
	public void createView(View view) {
		
		Log.d("MOONTEST", view.toString());
		
			try {
				Log.d("MOONTEST", "Show View");
				this.setContentView(view);
		    } catch (Exception e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		
	}
	
	public Context getCont() {
		
			try {
				
				return this.getApplicationContext();
		    } catch (Exception e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		    }
		
		return this.getApplicationContext();
		
	}
	
	
	@Override
	public InputStream findResource(String name) {
		try {
			return this.getApplicationContext().getAssets().open(name);
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}
}

```

##License
All dependencies hold their respective license. However, other parts of this repo written by me are free to use, modify and extend. 
