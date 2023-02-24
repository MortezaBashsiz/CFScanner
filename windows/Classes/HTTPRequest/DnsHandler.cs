using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Security;
using System.Reflection.Emit;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Threading.Tasks;

namespace WinCFScan.Classes.HTTPRequest
{
    public class DnsHandler : HttpClientHandler
    {
        private readonly CustomDnsResolver _dnsResolver;

        public DnsHandler(CustomDnsResolver dnsResolver)
        {
            _dnsResolver = dnsResolver;
        }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            // disable ssl cer check
            this.ServerCertificateCustomValidationCallback = HttpClientHandler.DangerousAcceptAnyServerCertificateValidator;

            var host = request.RequestUri.Host;
            var ip = _dnsResolver.Resolve(host);

            // add host to header
            request.Headers.TryAddWithoutValidation("host", host);

            var builder = new UriBuilder(request.RequestUri);
            builder.Host = ip;

            request.RequestUri = builder.Uri;

            return base.SendAsync(request, cancellationToken);
        }
    }

    public class CustomDnsResolver
    {
        private readonly string resolveTo;
        public CustomDnsResolver(string resolveTo)
        {
            this.resolveTo = resolveTo;
        }

        public string Resolve(string host)
        {
            return resolveTo;
        }
    }
}
