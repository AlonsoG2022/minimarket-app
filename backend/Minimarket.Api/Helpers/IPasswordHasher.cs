namespace Minimarket.Api.Helpers;

public interface IPasswordHasher
{
    string Hash(string input);
    bool Verify(string input, string hash);
}
