/*
 * SSIEmoVoiceTest.java
 * Copyright (c) 2018
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura,
 * Vitalijs Krumins, Antonio Grieco
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import hcm.ssj.audio.AudioChannel;
import hcm.ssj.audio.Microphone;
import hcm.ssj.core.Pipeline;
import hcm.ssj.ml.ClassifierT;
import hcm.ssj.ml.NaiveBayes;
import hcm.ssj.mobileSSI.SSI;
import hcm.ssj.mobileSSI.SSITransformer;
import hcm.ssj.test.Logger;

import static android.support.test.InstrumentationRegistry.getContext;

/**
 * Tests ssi emovoice component. Uses emovoice features and a simple naive bayes model to predict
 * positive or negative speech.
 * Created by Michael Dietz on 03.12.2018.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SSIEmoVoiceTest
{
    @Test
    public void testEmoVoice() throws Exception
    {
		//resources
		File dir = getContext().getFilesDir();
		String modelName = "emovoice.trainer";
		TestHelper.copyAssetToFile(modelName, new File(dir, modelName));
		TestHelper.copyAssetToFile("emovoice.model", new File(dir, "emovoice.model"));

        //setup
        Pipeline frame = Pipeline.getInstance();
        frame.options.bufferSize.set(10.0f);

        //sensor
        Microphone microphone = new Microphone();
        AudioChannel audioChannel = new AudioChannel();
        audioChannel.options.sampleRate.set(8000);
        audioChannel.options.scale.set(true);
        frame.addSensor(microphone, audioChannel);

        SSITransformer emovoiceFeatures = new SSITransformer();
        emovoiceFeatures.options.name.set(SSI.TransformerName.EmoVoiceFeat);
		emovoiceFeatures.options.ssioptions.set(new String[]{"maj->1", "min->0"});
        frame.addTransformer(emovoiceFeatures, audioChannel, 1.35);

		NaiveBayes naiveBayes = new NaiveBayes();
		naiveBayes.options.file.setValue(dir.getAbsolutePath() + File.separator + modelName);
		frame.addModel(naiveBayes);

		ClassifierT classifier = new ClassifierT();
		classifier.setModel(naiveBayes);
		frame.addTransformer(classifier, emovoiceFeatures, 1.35, 0);

        //logger
        Logger log = new Logger();
        //frame.addConsumer(log, emovoiceFeatures, 1, 0);
        frame.addConsumer(log, classifier, 1.35, 0);

        //start framework
        frame.start();
        //run test
        long end = System.currentTimeMillis() + TestHelper.DUR_TEST_NORMAL;
        try
        {
            while (System.currentTimeMillis() < end)
            {
                Thread.sleep(1);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        frame.stop();
        frame.release();
    }
}