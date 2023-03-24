class FileDownloadError(Exception):
    """Raised when a file download fails"""
    pass

class SubnetsReadError(Exception):
    """Raised when an error occurs while reading from asnlookup.com"""
    pass