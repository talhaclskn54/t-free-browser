# Free Browser

Opera'dan esinlenen özellikler: reklam engelleyici, gece modu, hızlı arama (speed dial) ana sayfası,
gizli sekme, çoklu sekme, veri tasarrufu, masaüstü sitesi, indirme yöneticisi, arama motoru seçimi.
Ayarlardan uygulama adı, renk ve logo anında değiştirilir.

## APK nasıl alınır (bilgisayarsız, ücretsiz)
1. github.com'da hesap aç, yeni bir depo (repository) oluştur.
2. Bu klasörün İÇİNDEKİ tüm dosyaları (`.github` klasörü dahil) depoya yükle.
3. Depoda **Actions** sekmesi -> **Build APK** -> çalışmasını bekle (~3-5 dk).
4. Bitince çalışmanın altındaki **FreeBrowser-apk** dosyasını indir, içinden APK çıkar, telefona kur.

Android Studio ile: klasörü aç -> Build > Build APK(s).

## Ana ekran ikonu / adı değiştirme (derleme öncesi)
- Ad: `app/src/main/res/values/strings.xml` içindeki `app_name`
- İkon: `app/src/main/res/drawable/ic_launcher.xml` (veya kendi PNG'ni `ic_launcher.png` adıyla koy, xml'i sil)
