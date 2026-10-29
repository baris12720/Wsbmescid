# WSB Hamburg Mescid TV — Android TV / Fire TV (APK)

Hazır web uygulamasını (`app/src/main/assets/www`) tam ekran açar. 7/24 çalışacak şekilde tasarlandı.
APK'yı GitHub üretir (bkz. `../GITHUB-KURULUM.md`); Android Studio ile de derlenebilir
(Open → bu klasör → Build → Build APK).

## Fire TV'ye yükleme
1. Fire TV: Ayarlar → Fire TV'im → Geliştirici Seçenekleri → **Bilinmeyen kaynaklardan uygulama yükle** açık.
   (Görünmüyorsa Hakkında → cihaz adına 7 kez bas.)
2. Fire TV'ye **Downloader** uygulamasını kurun, APK linkini yazıp indirin ve kurun.
3. Uygulamayı **bir kez elle açın**. (Fire TV, hiç açılmamış uygulamaya "açılışta başla" izni vermez.)

## Elektrik kesilince kendiliğinden açılması (bir kez)
Bilgisayardan (aynı Wi-Fi, Fire TV'de ADB hata ayıklama açık):
```
adb connect FIRE_TV_IP:5555
adb shell appops set org.wsb.mescidtv SYSTEM_ALERT_WINDOW allow
```
Bu izin, uygulamanın arka plandan ekranı açabilmesini sağlar. Sonra fişi çekip takarak test edin.

## Nasıl "kapanmaz"?
- Ekran uyumaz (pencere bayrağı + WakeLock).
- Bir **bekçi servisi** her 10 saniyede uygulamanın ekranda olup olmadığına bakar; 30 saniyeden uzun süre
  görünmezse (kapatıldıysa, çöktüyse, başka uygulamaya geçildiyse) ekranı geri getirir.
- WebView çökerse ekran otomatik yeniden kurulur. Her gece 03:00'te sayfa kendini tazeler.
- Elektrik gelince TV/Fire TV açılır, uygulama kendiliğinden başlar. Uygulama güncellenince de yeniden başlar.

## Bilerek kapatmak (bakım için)
**Kumandada GERİ (Back) tuşunu 3 saniye basılı tutun.** Uygulama kapanır ve bekçi durur; TV'nin ayarlarına girebilirsiniz.
Uygulamayı yeniden açtığınızda ya da TV yeniden başladığında kendiliğinden yine çalışır.
Alternatif: Fire TV → Ayarlar → Uygulamalar → Yüklü uygulamaları yönet → WSB Mescid TV → **Durmaya zorla**.

## Namaz vakitleri ve internet
- Uygulama yalnızca AlAdhan'dan **namaz vakitlerini** çeker (gün içinde 15 dakikada bir kontrol).
- Bu sırada **içinde bulunulan ay + sonraki 2 ay** indirilip TV'nin belleğine kaydedilir; internet kesilse bile
  yaklaşık 2-3 ay doğru (Diyanet) vakitler gösterilir. Bu süre dolarsa ya da veri yoksa, cihazın içindeki
  astronomik hesap devreye girer (Diyanet açıları, Hamburg; birkaç dakika sapma olabilir).
- Ayet ve hadisler (200+200) uygulamanın içindedir; internet gerekmez.

## Bayram, kandil ve Ramazan günleri
- Hicri takvim her gün internetten **Diyanet takvimiyle** (AlAdhan `calendarMethod=DIYANET`) güncellenir:
  bulunulan ay + sonraki 3 ay indirilir ve TV'nin belleğine kaydedilir. İnternet kesilse de kayıtlı resmi tarihler kullanılır.
- Veri alınamazsa ya da şüpheli gelirse (tarihler arası tutarsızlık) reddedilir; bu durumda yönetici panelindeki
  "Hicri tarih ayarı" değeri kullanılır.
- Cuma bilgisi ekrandan kaldırıldı.

## Android TV kutuları için isteğe bağlı: "ana ekran" modu
Fire TV'de çalışmaz; Google/Android TV kutularında Home tuşunun da bu uygulamayı açmasını isterseniz:
```
adb shell pm enable org.wsb.mescidtv/.HomeAlias
```
Sonra Home'a basıp "WSB Mescid TV"yi varsayılan seçin. Geri almak için `pm disable` komutu.

## TV ayarları
Ekran koruyucu kapalı · uyku süresi "hiçbir zaman" · HDMI-CEC kapalı (Fire TV'nin TV'yi kapatmasını önler) ·
tarih/saat otomatik, saat dilimi Berlin · TV'nin kendi "otomatik kapanma/enerji tasarrufu" ayarı kapalı.
