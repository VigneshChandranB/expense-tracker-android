@echo off
REM Final testing script for production release

echo ========================================
echo Expense Tracker - Final Testing Suite
echo ========================================
echo.

set FAILED_TESTS=0

echo [1/6] Running Unit Tests...
call gradlew test --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ Unit tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ Unit tests passed!
)
echo.

echo [2/6] Running Integration Tests...
call gradlew connectedAndroidTest --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ Integration tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ Integration tests passed!
)
echo.

echo [3/6] Running Performance Tests...
call gradlew testReleaseUnitTest --tests "*Performance*" --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ Performance tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ Performance tests passed!
)
echo.

echo [4/6] Running Security Tests...
call gradlew testReleaseUnitTest --tests "*Security*" --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ Security tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ Security tests passed!
)
echo.

echo [5/6] Running End-to-End Tests...
call gradlew connectedAndroidTest --tests "*EndToEnd*" --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ End-to-end tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ End-to-end tests passed!
)
echo.

echo [6/6] Running Regression Tests...
call gradlew connectedAndroidTest --tests "*Regression*" --continue
if %ERRORLEVEL% neq 0 (
    echo ❌ Regression tests failed!
    set /a FAILED_TESTS+=1
) else (
    echo ✅ Regression tests passed!
)
echo.

echo ========================================
echo Test Results Summary
echo ========================================

if %FAILED_TESTS% equ 0 (
    echo ✅ ALL TESTS PASSED! Ready for production release.
    echo.
    echo Next steps:
    echo 1. Run build-release.bat to generate production APK
    echo 2. Verify APK using verify-apk.bat
    echo 3. Complete final testing checklist
    echo 4. Submit for release approval
) else (
    echo ❌ %FAILED_TESTS% test suite(s) failed!
    echo.
    echo Please fix failing tests before proceeding with release.
    echo Check test reports in app/build/reports/tests/
)

echo.
echo Test reports available at:
echo - Unit Tests: app\build\reports\tests\testReleaseUnitTest\index.html
echo - Integration Tests: app\build\reports\androidTests\connected\index.html
echo.

pause