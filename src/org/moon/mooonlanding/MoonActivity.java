package org.moon.mooonlanding;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.moon.mooonlanding.lpeg;
import org.luaj.vm2.Globals;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import android.content.Context;
import android.content.DialogInterface;

public class MoonActivity extends Activity implements ResourceFinder {
	public Globals lua;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LuaValue activity = CoerceJavaToLua.coerce(this);
		LuaValue contxt = CoerceJavaToLua.coerce(this.getApplicationContext());
		lua = JsePlatform.standardGlobals();
		lua.finder = this;
		try{
			lua.set("AndroidContext", contxt);
			lua.load(new DebugLib());
			lua.loadfile("Moon.lua").call(activity);
		}catch(Exception e){
			//e.printStackTrace();
			//Log.d("MoonError", e.getMessage());
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
