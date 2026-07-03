using System.Security.Cryptography;
using System.Text;

namespace Minimarket.Api.Helpers;

// Cifrado simetrico (AES-256-CBC) para guardar secretos en la BD (ej. la clave de la IA).
// Compatible con el equivalente en Java (AES/CBC/PKCS5Padding, misma passphrase),
// para que ambos backends puedan descifrar el mismo valor.
public static class CryptoHelper
{
    private const string Passphrase = "Minimarket-Api-Secret-2026";

    private static byte[] Key => SHA256.HashData(Encoding.UTF8.GetBytes(Passphrase));

    public static string Encrypt(string plainText)
    {
        using var aes = Aes.Create();
        aes.Key = Key;
        aes.GenerateIV();

        using var encryptor = aes.CreateEncryptor();
        var plainBytes = Encoding.UTF8.GetBytes(plainText);
        var cipherBytes = encryptor.TransformFinalBlock(plainBytes, 0, plainBytes.Length);

        var result = new byte[aes.IV.Length + cipherBytes.Length];
        Buffer.BlockCopy(aes.IV, 0, result, 0, aes.IV.Length);
        Buffer.BlockCopy(cipherBytes, 0, result, aes.IV.Length, cipherBytes.Length);
        return Convert.ToBase64String(result);
    }

    public static string Decrypt(string cipherTextBase64)
    {
        var all = Convert.FromBase64String(cipherTextBase64);
        using var aes = Aes.Create();
        aes.Key = Key;

        var iv = new byte[16];
        Buffer.BlockCopy(all, 0, iv, 0, iv.Length);
        aes.IV = iv;

        using var decryptor = aes.CreateDecryptor();
        var plainBytes = decryptor.TransformFinalBlock(all, iv.Length, all.Length - iv.Length);
        return Encoding.UTF8.GetString(plainBytes);
    }
}
