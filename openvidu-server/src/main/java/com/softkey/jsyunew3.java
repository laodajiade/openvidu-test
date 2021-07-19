package com.softkey;

public class jsyunew3 {
    //获到锁的版本
    public static native int GetVersion(String InPath);

    //获到锁的扩展版本
    public static native int GetVersionEx(String InPath);

    //获到锁的ID
    public static native long GetID_1(String InPath);

    public static native long GetID_2(String InPath);

    //返回最后的错误信息
    public static native long get_LastError();

    //查找加密锁
    public static native String FindPort(int start);

    //查找指定的加密锁(使用普通算法一)
    public static native String FindPort_2(int start, long in_data, long verf_data);

    //查找指定的加密锁(使用普通算法二)
    public static native String FindPort_3(int start, long in_data, long verf_data);

    //设置读密码
    public static native int SetReadPassword(String W_hkey, String W_lkey, String new_hkey, String new_lkey, String InPath);

    //设置写密码
    public static native int SetWritePassword(String W_hkey, String W_lkey, String new_hkey, String new_lkey, String InPath);

    //普通算法函数
    public static native int sWriteEx_New(int in_data, String KeyPath);

    public static native int sWrite_2Ex_New(int in_data, String KeyPath);

    public static native int sWriteEx(int InData, String InPath);

    public static native int sWrite_2Ex(int InData, String InPath);

    public static native long sRead(String InPath);

    public static native int sWrite(long InData, String InPath);

    public static native int sWrite_2(long InData, String InPath);

    //从加密锁中读取一批字节
    public static native int YReadEx(short Address, short len, String HKey, String LKey, String InPath);

    //从加密锁中读取一个字节，一般不使用
    public static native int YRead(short Address, String HKey, String LKey, String InPath);

    //从缓冲区中获得数据
    public static native short GetBuf(int pos);

    //写一批字节到加密锁中
    public static native int YWriteEx(short Address, short len, String HKey, String LKey, String InPath);

    //写一个字节到加密锁中，一般不使用
    public static native int YWrite(short inData, short Address, String HKey, String LKey, String InPath);

    //设置要写入的缓冲区的数据
    public static native int SetBuf(int pos, short Data);

    //从加密锁中读字符串-新
    public static native String NewReadString(int Address, int len, String HKey, String LKey, String InPath);

    //写字符串到加密锁中-新
    public static native int NewWriteString(String InString, int Address, String HKey, String LKey, String InPath);

    //兼容旧的读写字符串函数，不再使用
    public static native String YReadString(short Address, short len, String HKey, String LKey, String InPath);

    public static native int YWriteString(String InString, short Address, String HKey, String LKey, String InPath);

    //'设置增强算法密钥一
    public static native int SetCal_2(String Key, String InPath);

    //使用增强算法一对字符串进行加密
    public static native String EncString(String InString, String InPath);

    //使用增强算法一对二进制数据进行加密
    public static native int Cal(String InPath);

    //'设置增强算法密钥二
    public static native int SetCal_New(String Key, String InPath);

    //使用增强算法二对字符串进行加密
    public static native String EncString_New(String InString, String InPath);

    //使用增强算法二对二进制数据进行加密
    public static native int Cal_New(String InPath);

    //使用增强算法对字符串进行解密
    public static native String DecString(String InString, String Key);

    //设置要加密的缓冲区的数据
    public static native int SetEncBuf(int pos, short Data);

    //从缓冲区中获取加密后的数据
    public static native short GetEncBuf(int pos);

    //返回加密锁的公钥对
    public static native String GetPubKeyX(String InPath);

    public static native String GetPubKeyY(String InPath);

    //生成SM2密钥对，获取私钥及公钥
    public static native String get_GenPriKey();

    public static native String get_GenPubKeyX();

    public static native String get_GenPubKeyY();

    //生成SM2密钥对,产生密钥对
    public static native int StarGenKeyPair(String InPath);

    public static native String SM2_EncString(String InString, String InPath);

    public static native String SM2_DecString(String InString, String Pin, String InPath);

    public static native int YtSetPin(String OldPin, String NewPin, String InPath);

    //对消息进行签名
    public static native String YtSign(String msg, String Pin, String InPath);

    //对签名进行验证
    public static native boolean YtVerfiy(String id, String msg, String PubKeyX, String PubKeyY, String VerfiySign, String InPath);

    //设置SM2密钥对及身份
    public static native int Set_SM2_KeyPair(String PriKey, String PubKeyX, String PubKeyY, String sm2UserName, String InPath);

    //获取加密锁中的身份
    public static native String GetSm2UserName(String InPath);

    //返回锁的硬件芯片唯一ID
    public static native String GetChipID(String InPath);

    //查找加密锁，返回的是U盘的路径,即U盘的盘符，通过这个路径也可以直接操作锁
    public static native String FindU(int start);

    //查找指定的加密锁（使得普通算法二），返回的是U盘的路径,即U盘的盘符，通过这个路径也可以直接操作锁
    public static native String FindU_3(int start, int in_data, int verf_data);

    //查找指定的加密锁（使得普通算法一），返回的是U盘的路径,即U盘的盘符，通过这个路径也可以直接操作锁
    public static native String FindU_2(int start, int in_data, int verf_data);

    //设置U盘部分为只读状态，
    public static native int SetUReadOnly(String InPath);

    //设置是否显示U盘部分盘符，真为显示，否为不显示
    public static native int SetHidOnly(boolean IsHidOnly, String InPath);

    //返回U盘部分是否为只读状态，真为只读
    public static native boolean IsUReadOnly(String InPath);

    //设置锁的ID
    public static native int SetID(String Seed, String InPath);

    //设置普通算法
    public static native int SetCal(String Old_hkey, String Old_lkey, String new_hkey, String new_lkey, String InPath);

    //获取出厂唯一编码
    public static native String GetProduceDate(String InPath);

    //***初始化加密锁函数
    public static native int ReSet(String InPath);

    //以下是代码下载及运行所需要的函数
    //查找D8加密狗
    public static native String FindD8(int pos, String VerfCode);

    //获取功能版本
    public static native int GetFuncVer(String InPath);

    //下载要运行的代码
    public static native int DownLoadBinFile(boolean bIsEnc, String BinFile, String InPath);

    public static native int DownLoadData(boolean bIsEnc, byte[] Buf, int BufLen, String InPath);

    public static native String EncBinFile(String BinFile, String Key);

    //运行函数
    public static native int RunFuntion(String FunctionName, String InPath);

    public static native int ContinuRun(String InPath);

    //设置初始化变量值到锁中
    public static native int SetVar(byte[] Buf, int MemBeginPos, int BufLen, String InPath);

    //从锁中返回运行结果
    public static native byte[] GetVar(int MemBeginPos, int OutBufLen, String InPath);

    //设置下载密钥
    public static native String SetDownLodKey(String OldKey, String NewKey, String InPath);

    public static native int OpenKey(String VerfCode, String InPath);//下载数据时，要先打开KEY，这个函数也可以用于验证是否是自己的KEY

    public static native int CloseKey(String InPath);//关闭KEY，用于禁止下载，要下载BIN文件到KYE时，必须要先打开


    //调用上位机API，
    public static native byte[] GetApiParam(int OutLen, String InPath);//获取要输入的参数数据

    public static native int SetApiParam(byte[] Buf, int InLen, String InPath);//设置要返回的参数数据

    //操作28K储存器
    public static native int WriteEprom(byte[] InBuf, int Addr, int len, String HKey, String LKey, String InPath);

    public static native byte[] YReadStringReadEprom(int Addr, int len, String HKey, String LKey, String InPath);

    public static native int NewSetReadPassword(String OldWriteHKey, String OldWriteLKey, String NewHKey, String NewLKey, String InPath);

    public static native int NewSetWritePassword(String OldWriteHKey, String OldWriteLKey, String NewHKey, String NewLKey, String InPath);

    //时间限制
    public static native int GetLimitYear(String InPath);

    public static native int GetLimitMonth(String InPath);

    public static native int GetLimitDay(String InPath);

    public static native int GetUserID(String InPath);

    public static native int GetLeaveNumber(String InPath);

    public static native int GetLeaveDays(String CurDate, String InPath);

    public static native int CheckBind(boolean bIsAdd, String MacAddr, String InPath);

    public static native int CheckNumber(String InPath);

    public static native int CheckDate(String InDate, String InPath);

    public static native int UpdateAuth(byte Flag, String Auth, String InPath);

    public static native int DateAuth(int CurYear, byte CurMonth, byte CurDay,
                                      int LimitYear, byte LimitMonth, byte LimitDay, int UserID,
                                      String Key, String InPath);

    public static native int NumberAuth(int Number, int UserID, String Key, String InPath);

    public static native int BindAuth(boolean bReBind, int BindCount, int UserID, String Key, String InPath);

    public static native int GetLimitBindCount(String InPath);

    public static native int GetAlreadyBindCoun(String InPath);

    public static native String MakeBindAuth(boolean bReBind, int BindCount, int UserID, String Key);

    public static native String MakeNumberAuth(int Number, int UserID, String Key);

    public static native String MakeDateAuth(int CurYear, byte CurMonth, byte CurDay,
                                             int LimitYear, byte LimitMonth, byte LimitDay, int UserID,
                                             String Key);

    public static native int CloseUsbHandle(String InPath);

    static {
        String DllName;
        String JdkBit = System.getProperty("sun.arch.data.model");
        if (JdkBit.equals("32")) {
            DllName = "Jsyunew3";

        } else {
            DllName = "Jsyunew3_64";

        }
        try {
            System.loadLibrary(DllName);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Can//t find library:" + DllName);
            System.exit(-1);
        }
    }

}