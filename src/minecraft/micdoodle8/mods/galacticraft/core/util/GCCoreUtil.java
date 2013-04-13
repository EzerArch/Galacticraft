package micdoodle8.mods.galacticraft.core.util;

import micdoodle8.mods.galacticraft.core.GCCoreThreadVersionCheck;
import cpw.mods.fml.relauncher.Side;

public class GCCoreUtil
{
	public static int convertTo32BitColor(int a, int r, int b, int g)
	{
        a = a << 24;
        r = r << 16;
        g = g << 8;

        return a | r | g | b;
	}

	public static void checkVersion(Side side)
    {
		GCCoreThreadVersionCheck.startCheck(side);

//    	try
//    	{
//    		final URL url = new URL("http://micdoodle8.com/galacticraft/version.html");
//
//    		final BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
//    		Pattern.compile("Version=");
//    		String str;
//    		String str2[] = null;
//
//    		while ((str = in.readLine()) != null)
//    		{
//	    		if (str.contains("Version"))
//	    		{
//	        		str = str.replace("Version=", "");
//
//		    		str2 = str.split("#");
//
//		    		if (str2 != null && str2.length == 3)
//		    		{
//		    			GalacticraftCore.remoteMajVer = Integer.parseInt(str2[0]);
//		    			GalacticraftCore.remoteMinVer = Integer.parseInt(str2[1]);
//		    			GalacticraftCore.remoteBuildVer = Integer.parseInt(str2[2]);
//		    		}
//
//		    		if (GalacticraftCore.remoteBuildVer != 0 && GalacticraftCore.remoteBuildVer < GalacticraftCore.LOCALBUILDVERSION)
//		    		{
//		    			GalacticraftCore.usingDevVersion = true;
//		    		}
//
//		    		if (GalacticraftCore.remoteMajVer > GalacticraftCore.LOCALMAJVERSION || GalacticraftCore.remoteMajVer == GalacticraftCore.LOCALMAJVERSION && GalacticraftCore.remoteMinVer > GalacticraftCore.LOCALMINVERSION || GalacticraftCore.remoteMajVer == GalacticraftCore.LOCALMAJVERSION && GalacticraftCore.remoteMinVer == GalacticraftCore.LOCALMINVERSION && GalacticraftCore.remoteBuildVer > GalacticraftCore.LOCALBUILDVERSION)
//		    		{
//		    			if (side.equals(Side.CLIENT))
//		    			{
//		    				FMLClientHandler.instance().getClient().thePlayer.addChatMessage("\u00a77New \u00a73Galacticraft \u00a77version available! v" + String.valueOf(GalacticraftCore.remoteMajVer) + "." + String.valueOf(GalacticraftCore.remoteMinVer) + "." + String.valueOf(GalacticraftCore.remoteBuildVer) + " \u00a71http://micdoodle8.com/");
//		    			}
//		    			else if (side.equals(Side.SERVER))
//		    			{
//		    				GCLog.severe("New Galacticraft version available! v" + String.valueOf(GalacticraftCore.remoteMajVer) + "." + String.valueOf(GalacticraftCore.remoteMinVer) + "." + String.valueOf(GalacticraftCore.remoteBuildVer) + " http://micdoodle8.com/");
//		    			}
//		    		}
//	    		}
//    		}
//    	}
//    	catch (final MalformedURLException e)
//    	{
//    		e.printStackTrace();
//
//			if (side.equals(Side.CLIENT))
//			{
//	    		FMLClientHandler.instance().getClient().thePlayer.addChatMessage("[Galacticraft] Update Check Failed!");
//			}
//
//    		GCLog.severe("Galacticraft Update Check Failure - MalformedURLException");
//    	}
//    	catch (final IOException e)
//    	{
//    		e.printStackTrace();
//
//			if (side.equals(Side.CLIENT))
//			{
//	    		FMLClientHandler.instance().getClient().thePlayer.addChatMessage("[Galacticraft] Update Check Failed!");
//			}
//
//    		GCLog.severe("Galacticraft Update Check Failure - IOException");
//    	}
//    	catch (final NumberFormatException e)
//    	{
//    		e.printStackTrace();
//
//			if (side.equals(Side.CLIENT))
//			{
//	    		FMLClientHandler.instance().getClient().thePlayer.addChatMessage("[Galacticraft] Update Check Failed!");
//			}
//
//    		GCLog.severe("Galacticraft Update Check Failure - NumberFormatException");
//    	}
    }
}
