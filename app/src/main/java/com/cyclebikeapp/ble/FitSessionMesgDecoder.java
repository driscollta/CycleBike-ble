package com.cyclebikeapp.ble;

import com.garmin.fit.Decode;
import com.garmin.fit.FitRuntimeException;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SessionMesgListener;

import java.io.FileInputStream;

/**
 * Created by TommyD on 12/20/2015.
 *
 */
class FitSessionMesgDecoder {
    private double totalDistance = 0;
    private class Listener implements SessionMesgListener {

        @Override
        public void onMesg(SessionMesg mesg) {
            if(mesg.getTotalDistance() != null){
                System.out.print("   TotalDistance: ");
                System.out.println(mesg.getTotalDistance());
                totalDistance = mesg.getTotalDistance();
            }
        }
    }

    double decodeFile(String args) {
        Decode decode = new Decode();

        //decode.skipHeader();        // Use on streams with no header and footer (stream contains FIT defn and data messages only)
        decode.incompleteStream();  // This suppresses exceptions with unexpected eof (also incorrect crc)
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);
        Listener listener = new Listener();
        FileInputStream in;

        try {
            in = new FileInputStream(args);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error opening file " + args + " [1]");
        }

        try {
            if (!decode.checkFileIntegrity(in))
                throw new RuntimeException("FIT file integrity failed.");
        }  catch (RuntimeException e) {
            e.printStackTrace();
            //System.err.print("Exception Checking File Integrity: ");

            //System.err.println("Trying to continue...");
        }
        finally {
            try {
                in.close();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            in = new FileInputStream(args);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error opening file " + args + " [2]");
        }

        mesgBroadcaster.addListener( listener);

        try {
            decode.read(in, mesgBroadcaster, mesgBroadcaster);
        } catch (FitRuntimeException e) {
            // If a file with 0 data size in it's header  has been encountered,
            // attempt to keep processing the file
            if (decode.getInvalidFileDataSize()) {
                decode.nextFile();
                decode.read(in, mesgBroadcaster, mesgBroadcaster);
            }
            else {
                System.err.print("Exception decoding file: ");
                System.err.println(e.getMessage());

                try {
                    in.close();
                } catch (java.io.IOException f) {
                    throw new RuntimeException(f);
                }

                return totalDistance;
            }
        }

        try {
            in.close();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        //System.out.println("Decoded FIT file " + args + ".");
        return totalDistance;
    }
}
