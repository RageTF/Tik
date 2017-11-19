package coder;

import java.io.*;
import java.util.*;

public class Coder {

    private static int mByteMask = Integer.parseInt("11111111", 2);

    public static class CodeSymbol {
        private byte[] mBytes;

        public CodeSymbol(byte[] bytes) {
            mBytes = bytes;
        }

        public byte[] getBytes() {
            return mBytes;
        }

        public void setBytes(byte[] bytes) {
            mBytes = bytes;
        }

        @Override
        public int hashCode() {
            return mBytes[0];
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CodeSymbol))
                return false;
            return Arrays.equals(mBytes, ((CodeSymbol) obj).mBytes);
        }
    }

    public static void code(Map<CodeSymbol, String> codeSymbolMap, int countByteInSymbol, InputStream src, OutputStream dest) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(src);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(dest);
        DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
        DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
        int countSymbols = codeSymbolMap.size();
        dataOutputStream.writeInt(countSymbols);
        dataOutputStream.writeByte(countByteInSymbol);
        codeSymbolMap
                .forEach((codeSymbol, s) -> {
                    try {
                        for (int i = 0; i < countByteInSymbol; i++) {
                            dataOutputStream.writeByte(codeSymbol.mBytes[i]);
                        }
                        dataOutputStream.writeByte(s.length());
                        StringBuilder stringBuilder = new StringBuilder(s);
                        while (stringBuilder.length() >= 8) {
                            String b = stringBuilder.substring(0, 8);
                            stringBuilder.delete(0, 8);
                            dataOutputStream.writeByte(Integer.parseInt(b, 2));
                        }
                        if (stringBuilder.length() > 0) {
                            dataOutputStream.writeByte(Integer.parseInt(stringBuilder.toString(), 2));
                        }
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        int b;
        StringBuilder writeBytes = new StringBuilder();
        byte[] bytesArray = new byte[countByteInSymbol];
        int count = 0;
        while ((b = dataInputStream.read()) != -1) {
            if (count == countByteInSymbol) {
                String s = codeSymbolMap.get(new CodeSymbol(bytesArray));
                writeBytes.append(s);
                while (writeBytes.length() >= 8) {
                    dataOutputStream.write((byte) Integer.parseInt(writeBytes.substring(0, 8), 2));
                    writeBytes.delete(0, 8);
                }
                bytesArray = new byte[countByteInSymbol];
                count = 0;
            } else {
                bytesArray[count++] = (byte) b;
            }
        }
        int emptyPostfix = 8 - writeBytes.length();
        for (int i = 0; i < emptyPostfix; i++) {
            writeBytes.append("0");
        }
        dataOutputStream.write((byte) Integer.parseInt(writeBytes.substring(0, 8), 2));
        dataOutputStream.write((byte) emptyPostfix);
        for(int i=0;i<countByteInSymbol-1;i++){
            if(i<count){
                dataOutputStream.writeByte(bytesArray[i]);
            }else{
                dataOutputStream.writeByte(0);
            }
        }
        dataOutputStream.flush();
    }

    public static void encode(DataInputStream dataInputStream,DataOutputStream dataOutputStream){

    }

}
