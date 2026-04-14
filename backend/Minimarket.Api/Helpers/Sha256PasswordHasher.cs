using System.Security.Cryptography;
using System.Text;

namespace Minimarket.Api.Helpers;

public class Sha256PasswordHasher : IPasswordHasher
{
    public string Hash(string input)
    {
        var bytes = SHA256.HashData(Encoding.UTF8.GetBytes(input));
        return Convert.ToHexString(bytes).ToLowerInvariant();
    }

    public bool Verify(string input, string hash) => Hash(input) == hash;
}
