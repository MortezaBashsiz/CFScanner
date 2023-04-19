import functools
from threading import Thread


def timeout_fun(timeout: float):
    """Timeout function decorator.
    It will raise a TimeoutError if the function takes longer than `timeout` seconds to complete.     

    Args:
        timeout (float): timeout in seconds
    
    Returns:
        function: the decorated function
    """    
    def deco(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            res = [TimeoutError(f'Timeout {timeout} exceeded')]

            def newFunc():
                try:
                    res[0] = func(*args, **kwargs)
                except Exception as e:
                    res[0] = e
            t = Thread(target=newFunc)
            t.daemon = True
            try:
                t.start()
                t.join(timeout)
            except Exception as je:
                print('error starting thread')
                raise je
            ret = res[0]
            if isinstance(ret, BaseException):
                raise ret
            return ret
        return wrapper
    return deco
