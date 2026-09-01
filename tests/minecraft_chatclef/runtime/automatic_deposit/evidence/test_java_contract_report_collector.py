#20260831_kpopmodder: Lock real JUnit testcase and clean-build provenance ownership.
from __future__ import annotations

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from types import SimpleNamespace

from .java_contract_report_collector import (
    automatic_deposit_java_contract_test_cases,
    collect_automatic_deposit_java_contract_report,
)


class AutomaticDepositJavaContractReportCollectorTests(unittest.TestCase):
    def test_canonical_passing_junit_cases_create_sealed_manifest(self):
        keys = ("generation_boundary_verified",)
        raw = _junit_xml(keys)

        manifest, reason = _collect(raw, keys)

        self.assertEqual("JAVA_CONTRACT_JUNIT_REPORTS_COLLECTED", reason)
        self.assertIsNotNone(manifest)
        self.assertEqual(
            (("generation_boundary_verified", "true"),),
            manifest.results,
        )
        self.assertEqual("clean build --rerun-tasks", manifest.gradle_invocation)

    def test_failed_required_testcase_is_derived_as_false(self):
        keys = ("one_tick_barrier_verified",)
        failed_case = automatic_deposit_java_contract_test_cases(keys)[0]
        raw = _junit_xml(keys, failed_case=failed_case)

        manifest, reason = _collect(raw, keys)

        self.assertEqual("JAVA_CONTRACT_JUNIT_REPORTS_COLLECTED", reason)
        self.assertEqual(
            (("one_tick_barrier_verified", "false"),),
            manifest.results,
        )

    def test_skipped_required_testcase_is_inconclusive(self):
        keys = ("one_tick_barrier_verified",)
        skipped_case = automatic_deposit_java_contract_test_cases(keys)[0]
        raw = _junit_xml(keys, skipped_case=skipped_case)

        manifest, reason = _collect(raw, keys)

        self.assertIsNone(manifest)
        self.assertEqual("JAVA_CONTRACT_REQUIRED_TESTCASE_SKIPPED", reason)

    def test_missing_clean_build_provenance_is_inconclusive(self):
        keys = ("generation_boundary_verified",)
        raw = _junit_xml(keys, omit_property="lavi.gradleInvocation")

        manifest, reason = _collect(raw, keys)

        self.assertIsNone(manifest)
        self.assertEqual("JAVA_CONTRACT_PROVENANCE_MISSING", reason)

    def test_noncanonical_report_directory_is_rejected_before_scan(self):
        scans: list[str] = []
        manifest, reason = collect_automatic_deposit_java_contract_report(
            "C:/outside/test-results",
            ("generation_boundary_verified",),
            report_paths_reader=lambda path: scans.append(str(path)),
        )

        self.assertIsNone(manifest)
        self.assertEqual(
            "JAVA_CONTRACT_REPORT_DIRECTORY_NOT_CANONICAL",
            reason,
        )
        self.assertEqual([], scans)


def _collect(raw: bytes, keys: tuple[str, ...]):
    report = _report_directory().joinpath("TEST-hermetic.xml")
    stat = SimpleNamespace(
        st_dev=1,
        st_ino=2,
        st_size=len(raw),
        st_mtime_ns=3,
    )
    return collect_automatic_deposit_java_contract_report(
        _report_directory(),
        keys,
        report_paths_reader=lambda _directory: (report,),
        bytes_reader=lambda _path: raw,
        stat_reader=lambda _path: stat,
    )


def _report_directory() -> Path:
    return Path(__file__).resolve().parents[5].joinpath(
        "plugins",
        "Minecraft",
        "runtime",
        "chatclef_fabric_1.20.1",
        "build",
        "test-results",
        "test",
    )


def _junit_xml(
    keys: tuple[str, ...],
    *,
    failed_case: tuple[str, str] | None = None,
    skipped_case: tuple[str, str] | None = None,
    omit_property: str = "",
) -> bytes:
    cases = automatic_deposit_java_contract_test_cases(keys)
    suite = ET.Element(
        "testsuite",
        {
            "name": "automatic-deposit-contract",
            "tests": str(len(cases)),
            "failures": "1" if failed_case else "0",
            "errors": "0",
            "skipped": "1" if skipped_case else "0",
        },
    )
    properties = ET.SubElement(suite, "properties")
    values = {
        "lavi.provenanceSchema": "automatic-deposit-junit/v1",
        "lavi.gitCommit": "2" * 40,
        "lavi.sourceJarSha256": "3" * 64,
        "lavi.gradleInvocation": "clean build --rerun-tasks",
        "lavi.testTask": "test",
        "lavi.testRunId": "hermetic-run-1",
    }
    for name, value in values.items():
        if name != omit_property:
            ET.SubElement(
                properties,
                "property",
                {"name": name, "value": value},
            )
    for classname, name in cases:
        testcase = ET.SubElement(
            suite,
            "testcase",
            {"classname": classname, "name": f"{name}()"},
        )
        if (classname, name) == failed_case:
            ET.SubElement(testcase, "failure", {"message": "failed"})
        if (classname, name) == skipped_case:
            ET.SubElement(testcase, "skipped")
    return ET.tostring(suite, encoding="utf-8", xml_declaration=True)


if __name__ == "__main__":
    unittest.main()
