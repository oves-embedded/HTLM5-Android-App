package com.oves.app.util;


import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;


import com.oves.app.entity.PhoneDomain;

import java.util.ArrayList;
import java.util.List;

public class PhoneUtil {

    @SuppressLint("Range")
    public static List<PhoneDomain> readContacts(Context context) {
        List<PhoneDomain> list = new ArrayList<PhoneDomain>();
        ContentResolver contentResolver = context.getContentResolver();
        // 查询所有联系人的基本信息
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        try{
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    PhoneDomain domain = new PhoneDomain();
                    // 获取联系人姓名
                    @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    domain.setName(displayName);
                    // 获取联系人ID
                    @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));


                    // 查询该联系人的电话号码
                    Cursor phoneCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?",
                            new String[]{contactId},
                            null);
                    String phoneNumber = "";
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        do {
                            // 获取电话号码
                            phoneNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            domain.setPhoneNumber(phoneNumber);
                            // 这里可以根据需要处理电话号码
                        } while (phoneCursor.moveToNext());
                        phoneCursor.close();
                    }
                    list.add(domain);
                    // 可以继续查询联系人的其他信息，如邮箱等

                } while (cursor.moveToNext());

            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                cursor.close();
            }catch (Exception e){}
        }
        return list;
    }
}
