package.preload['lpeg'] = function (...)
    return require "org.moon.mooonlanding.lpeg"
end

activity = ...

--[[==[[local getScript = function(script)
  return activity:findResource(script)
end--]]

local moonscript = require("moonscript.base")
local parse = require("moonscript.parse")
local MoonApp = activity:readFile("init.moon")
local prs = parse.string(MoonApp)
prs()
local run = moonscript.loadstring(MoonApp)
return run()
