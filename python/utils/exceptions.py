class FileDownloadError(Exception):
    """Raised when a file download fails"""
    pass


class SubnetsReadError(Exception):
    """Raised when an error occurs while reading from asnlookup.com"""
    pass


class TemplateReadError(Exception):
    """Raised when an error occurs while reading a template file"""
    pass


class BinaryNotFoundError(Exception):
    """Raised when the xray binary file is not found"""
    pass
